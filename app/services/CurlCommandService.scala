/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.inject.Singleton
import io.swagger.oas.inflector.examples.ExampleBuilder
import io.swagger.oas.inflector.processors.JsonNodeExampleSerializer
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem}
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import models.CurlCommand.missingValue
import models.api.ApiDetail
import models.application.Application
import models.application.ApplicationLenses.*
import models.{ApiWorld, CORPORATE, CurlCommand, MDTP}
import play.api.libs.json.Json

import java.util.Base64.getEncoder
import scala.jdk.CollectionConverters.*

@Singleton
class CurlCommandService {

  def buildCurlCommandForEndpoint(application: Application, apiDetail: ApiDetail, path: String, method: String, apiWorld: ApiWorld): Either[String, CurlCommand] = {
    for {
      openApiDoc <- OpenApiDoc.parse(apiDetail.openApiSpecification)
      operation <- openApiDoc.getOperation(method, path)
    } yield CurlCommand(
      method = method,
      server = openApiDoc.getServerForApiWorld(apiWorld),
      path = path,
      queryParams = operation.queryParams,
      pathParams = operation.pathParams,
      headers = operation.headerParams ++ getCommonHeaders(application),
      requestBody = operation.exampleRequestBody
    )
  }

  private def getCommonHeaders(application: Application): Map[String,String] = {
    val maybeAuthHeader = for {
      credential <- application.getSecondaryMasterCredential
      secret <- credential.clientSecret
      credentials = s"${credential.clientId}:$secret"
      encodedCredentials = getEncoder.encodeToString(credentials.getBytes)
    } yield s"Basic $encodedCredentials"

    Map(
      "Content-Type" -> Some("application/json"),
      "Authorization" -> maybeAuthHeader
    ).collect({ case (key, Some(value)) => key -> value })
  }
}

class OpenApiDoc(openApi: OpenAPI) {
  private val schemas = openApi.getComponents.getSchemas

  def getServerForApiWorld(apiWorld: ApiWorld): Option[Server] = {
    val servers = openApi.getServers.asScala
    apiWorld match {
      case MDTP => servers.find(s => Option(s.getDescription).contains("MDTP - QA"))
      case CORPORATE => servers.find(s => Option(s.getDescription).contains("Corporate - Test"))
      case _ => None
    }
  }

  def getOperation(method: String, path: String): Either[String, OpenApiOperation] = {
    val paths = openApi.getPaths.asScala
    for {
      pathItem <- paths.get(path).toRight(s"No path matching '$path' was found in the OAS document")
      operation <- getOperationForMethod(method, pathItem).toRight(s"No $method operation found for the path '$path' in the OAS document'")
    } yield OpenApiOperation(operation)
  }

  private def getOperationForMethod(method: String, pathItem: PathItem): Option[Operation] = {
    Option(method match {
      case "GET" => pathItem.getGet
      case "POST" => pathItem.getPost
      case "PUT" => pathItem.getPut
      case "DELETE" => pathItem.getDelete
      case "OPTIONS" => pathItem.getOptions
      case "HEAD" => pathItem.getHead
      case "PATCH" => pathItem.getPatch
      case "TRACE" => pathItem.getTrace
    })
  }

  class OpenApiOperation(operation: Operation) {
    val queryParams = filterParameters(operation, "query")
    val pathParams = filterParameters(operation, "path", true)
    val headerParams = filterParameters(operation, "header")

    val exampleRequestBody = for {
      requestBody <- Option(operation.getRequestBody)
      content <- Option(requestBody.getContent)
      jsonContent <- content.asScala.get("application/json")
      schema <- Option(jsonContent.getSchema)
      example = ExampleBuilder.fromSchema(schema, schemas)
      examplePrettyJson = io.swagger.util.Json.pretty(example)
      exampleFlatJson = Json.parse(examplePrettyJson).toString
    } yield exampleFlatJson

    private def filterParameters(operation: Operation, in: String, includeNameInValue: Boolean = false): Map[String, String] = {
      Option(operation.getParameters).map(_.asScala).getOrElse(Seq.empty)
        .filter(_.getIn == in)
        .map(param => param.getName -> (if (includeNameInValue) missingValue(param.getName, param.getSchema.getType) else missingValue(param.getSchema.getType)))
        .toMap
    }
  }
}

object OpenApiDoc {
  private val simpleModule = SimpleModule().addSerializer(JsonNodeExampleSerializer());
  io.swagger.util.Json.mapper().registerModule(simpleModule);

  def parse(openApiSpecification: String): Either[String, OpenApiDoc] = {
    val options: ParseOptions = new ParseOptions()
    options.setResolve(false)

    val parseResult = new OpenAPIV3Parser().readContents("asdasda", null, options)
    parseResult.getOpenAPI match {
      case null => Left(("Unable to parse the OAS document" + parseResult.getMessages.asScala).mkString(", "))
      case openApi => Right(OpenApiDoc(openApi))
    }
  }
}
