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
          val authHeaderValue = request.application.getSecondaryMasterCredential.map(credential => s"${credential.clientId}:${credential.clientSecret.getOrElse("")}")
            .map(value => java.util.Base64.getEncoder.encodeToString(value.getBytes))
            .getOrElse("")

          val commands = applicationApis
            .flatMap(api => parseApiOasSpec(api.apiDetail, apiWorld))
            .map(command => command.copy(headers = command.headers +
              ("Authorization" -> s"Basic $authHeaderValue") +
              ("Content-Type" -> "application/json")
            ))
          Ok(Json.toJson(commands.map(_.toString)))
        }
        case Left(_) => Ok(Json.toJson(""))
      }
  }

  private def parseApiOasSpec(apiDetail: ApiDetail, apiWorld: ApiWorld): Seq[CurlCommand] = {
    val options: ParseOptions = new ParseOptions()

    options.setResolve(false)
    val result = new OpenAPIV3Parser().readContents(apiDetail.openApiSpecification, null, options)
    val openApiDoc = result.getOpenAPI
    val schemas = openApiDoc.getComponents.getSchemas
    val paths = openApiDoc.getPaths
    val allServers = openApiDoc.getServers.asScala
    val server = getServerForApiWorld(apiWorld, Option(allServers.toList))

    getCurlCommandsForPaths(paths)
      .map(command => command.copy(server = server.map(_.getUrl).getOrElse("")))
      .map(command => {
        val schemaMap = schemas.asScala.toMap
        val maybeRequestBody = Option(command.operation.getRequestBody)
        val maybeSchemaName = maybeRequestBody
          .flatMap(requestBody => Option(requestBody.getContent)
            .map(content => content.get("application/json"))
            .map(_.getSchema.get$ref.split("/").last)
          )
        getExampleRequestBodyJson(schemaMap, maybeSchemaName) match {
          case Some(requestBody) => command.copy(requestBody = Some(requestBody))
          case None => command
        }
      })

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

  private def getCurlCommandsForPaths(paths: Paths): Seq[CurlCommand] = {
    val x = paths.keySet().asScala.toSeq
    x.flatMap(path => getCurlCommandsForPath(path, paths.get(path)))
  }

  private def getCurlCommandsForPath(path: String, pathItem: PathItem): Seq[CurlCommand] = {
    Map(
      "GET" -> pathItem.getGet,
      "POST" -> pathItem.getPost,
      "PUT" -> pathItem.getPut,
      "DELETE" -> pathItem.getDelete,
      "OPTIONS" -> pathItem.getOptions,
      "HEAD" -> pathItem.getHead,
      "PATCH" -> pathItem.getPatch,
      "TRACE" -> pathItem.getTrace
    )
      .filter { case (_, operation) => operation != null }
      .map { case (method, operation) => getCurlCommandForOperation(method, path, pathItem, operation) }
      .toSeq
  }

  private def filterParameters(operation: Operation, in: String): Map[String,String] = {
    Option(operation.getParameters).map(_.asScala).getOrElse(Seq.empty)
      .filter(_.getIn == in)
      .map(param => param.getName -> param.getSchema.getType.toUpperCase)
      .toMap
  }

  private def getCurlCommandForOperation(method: String, path: String, pathItem: PathItem, operation: Operation): CurlCommand = {
    val queryParams = filterParameters(operation, "query")
    val pathParams = filterParameters(operation, "path")
    val headerParams = filterParameters(operation, "header")

    CurlCommand(method = method, path = path, queryParams = queryParams, pathParams = pathParams, headers = headerParams, operation = operation)
  }

}

case class CurlCommand(
                        method: String,
                        server: String = "",
                        path: String,
                        operation: Operation,
                        queryParams: Map[String, String] = Map.empty,
                        pathParams: Map[String, String] = Map.empty,
                        headers: Map[String, String] = Map.empty,
                        requestBody: Option[String] = None) {
  override def toString: String = {
    val hostAndPath = server + addPathParameters
    val url = uri"$hostAndPath?$queryParams"
    val headers = getHeadersString
    s"curl -X '${method}' $headers '$url' ${requestBody.map(json => s"--data '$json'").getOrElse("")}"
  }

  private def getHeadersString: String = {
    headers.map { case (key, value) => s"-H '$key: $value'" }.mkString(" ")
  }

  private def addPathParameters: String = {
    var newPath = path
    pathParams.foreach { case (key, value) =>
      newPath = newPath.replace(s"{$key}", value)
    }
    newPath
  }
}