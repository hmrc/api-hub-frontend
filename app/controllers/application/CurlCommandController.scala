/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.application

import com.google.inject.Inject
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ApplicationApiBuilder
import models.application.ApplicationLenses.*
import models.application.Application
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.ApplicationDetailsView
import models.api.ApiDetail

import scala.concurrent.ExecutionContext
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.oas.models.{Operation, PathItem, Paths}
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.media.Schema
import io.swagger.oas.inflector.examples.models.Example
import io.swagger.oas.inflector.examples.ExampleBuilder
import com.fasterxml.jackson.databind.module.SimpleModule
import io.swagger.oas.inflector.processors.JsonNodeExampleSerializer

import scala.jdk.CollectionConverters.*
import models.{ApiWorld, CORPORATE, MDTP}
import sttp.model.Uri.UriContext
import viewmodels.application.ApplicationEndpoint

class CurlCommandController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: ApplicationDetailsView,
  applicationApiBuilder: ApplicationApiBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def buildCurlCommand(id: String, apiWorld: ApiWorld): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      applicationApiBuilder.build(request.application).map {
        case Right(applicationApis) => {
          val commands = applicationApis.flatMap(appApi => appApi.endpoints.map(endpoint => buildCurlCommandForEndpoint(request.application, endpoint, appApi.apiDetail, apiWorld)))
          Ok(Json.toJson(commands.map(_.toString)))
        }
        case Left(_) => Ok(Json.toJson(""))
      }
  }

  private def buildCurlCommandForEndpoint(application: Application, endpoint: ApplicationEndpoint, apiDetail: ApiDetail, apiWorld: ApiWorld): CurlCommand = {
    val options: ParseOptions = new ParseOptions()
    options.setResolve(false)
    val parseResult = new OpenAPIV3Parser().readContents(apiDetail.openApiSpecification, null, options)
    val openApiDoc = parseResult.getOpenAPI
    val schemas = openApiDoc.getComponents.getSchemas
    val paths = openApiDoc.getPaths
    val allServers = openApiDoc.getServers.asScala
    val server = getServerForApiWorld(apiWorld, Option(allServers.toList))

    val operation = getOperationForEndpoint(endpoint, paths)

    val queryParams = filterParameters(operation, "query")
    val pathParams = filterParameters(operation, "path")
    val headerParams = filterParameters(operation, "header") +
        ("Authorization" -> s"Basic ${getAuthHeaderForApplication(application)}") +
        ("Content-Type" -> "application/json")

    val exampleRequestBody = getExampleRequestBody(schemas.asScala.toMap, operation)

    CurlCommand(
      method = endpoint.httpMethod,
      server = server.map(_.getUrl).getOrElse(""),
      path = endpoint.path,
      queryParams = queryParams,
      pathParams = pathParams,
      headers = headerParams,
      requestBody = exampleRequestBody
    )
  }

  private def getAuthHeaderForApplication(application: Application): String = {
    application.getSecondaryMasterCredential.map(credential => s"${credential.clientId}:${credential.clientSecret.getOrElse("")}")
      .map(value => java.util.Base64.getEncoder.encodeToString(value.getBytes))
      .getOrElse("")
  }

  private def getOperationForEndpoint(endpoint: ApplicationEndpoint, paths: Paths): Operation = {
    val pathItem = paths.get(endpoint.path)
    Map(
      "GET" -> pathItem.getGet,
      "POST" -> pathItem.getPost,
      "PUT" -> pathItem.getPut,
      "DELETE" -> pathItem.getDelete,
      "OPTIONS" -> pathItem.getOptions,
      "HEAD" -> pathItem.getHead,
      "PATCH" -> pathItem.getPatch,
      "TRACE" -> pathItem.getTrace
    ).get(endpoint.httpMethod).get
  }
  
  private def getExampleRequestBody(schemas: Map[String, Schema[?]], operation: Operation): Option[String] = {
    val schemaMap = schemas
    val maybeRequestBody = Option(operation.getRequestBody)
    val maybeSchemaName = maybeRequestBody
      .flatMap(requestBody => Option(requestBody.getContent)
        .map(content => content.get("application/json"))
        .map(_.getSchema.get$ref.split("/").last)
      )
    getExampleRequestBodyJson(schemaMap, maybeSchemaName)
  }

  private def getServerForApiWorld(apiWorld: ApiWorld, maybeServers: Option[List[Server]]): Option[Server] = {
    maybeServers match {
      case Some(servers) =>     apiWorld match {
        case MDTP => servers.find(s => Option(s.getDescription).contains("MDTP - QA"))
        case CORPORATE => servers.find(s => Option(s.getDescription).contains("Corporate - Test"))
        case _ => None
      }
      case None => None
    }
  }

  private def getExampleRequestBodyJson(schemas: Map[String, Schema[?]], schemaName: Option[String]): Option[String] = {
    schemas.get(schemaName.getOrElse("")).map(schema => {
      val example = ExampleBuilder.fromSchema(schema, schemas.asJava);
      val simpleModule = SimpleModule().addSerializer(JsonNodeExampleSerializer());
      io.swagger.util.Json.mapper().registerModule(simpleModule);
      val x = Json.parse(io.swagger.util.Json.pretty(example))
      x.toString()
    })
  }

  private def filterParameters(operation: Operation, in: String): Map[String,String] = {
    Option(operation.getParameters).map(_.asScala).getOrElse(Seq.empty)
      .filter(_.getIn == in)
      .map(param => param.getName -> param.getSchema.getType.toUpperCase)
      .toMap
  }

}

case class CurlCommand(
                        method: String,
                        server: String = "",
                        path: String,
                        queryParams: Map[String, String] = Map.empty,
                        pathParams: Map[String, String] = Map.empty,
                        headers: Map[String, String] = Map.empty,
                        requestBody: Option[String] = None) {
  override def toString: String = {
    val hostAndPath = server.replaceAll("/$", "") + getPathWithParameters
    val url = uri"$hostAndPath?$queryParams"
    val headers = getHeadersString
    s"curl -X '${method}' $headers '$url' ${requestBody.map(json => s"--data '$json'").getOrElse("")}"
  }

  private def getHeadersString: String = {
    headers.map { case (key, value) => s"-H '$key: $value'" }.mkString(" ")
  }

  private def getPathWithParameters: String = {
    var newPath = path
    pathParams.foreach { case (key, value) =>
      newPath = newPath.replace(s"{$key}", value)
    }
    newPath
  }
}