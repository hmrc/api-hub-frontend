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

package models.curl

import com.fasterxml.jackson.databind.module.SimpleModule
import io.swagger.oas.inflector.examples.ExampleBuilder
import io.swagger.oas.inflector.processors.JsonNodeExampleSerializer
import io.swagger.v3.oas.models.{Components, OpenAPI, Operation, PathItem, Paths}
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.{ParseOptions, SwaggerParseResult}
import models.CurlCommand.missingValue
import models.{ApiWorld, CORPORATE, MDTP}
import play.api.Logging
import play.api.i18n.{Messages, MessagesProvider}
import play.api.libs.json.Json

import scala.jdk.CollectionConverters.*
import scala.util.Try

class OpenApiDoc(openApi: OpenAPI) {
  private val schemas = Option(openApi.getComponents).getOrElse(Components()).getSchemas

  def getServerForApiWorld(apiWorld: ApiWorld): Option[Server] = {
    val servers = Option(openApi.getServers).map(_.asScala).getOrElse(Seq.empty)

    apiWorld match {
      case MDTP => servers.find(s => Option(s.getDescription).contains("MDTP - QA"))
      case CORPORATE => servers.find(s => Option(s.getDescription).contains("Corporate - Test"))
    }
  }

  def getOperation(method: String, path: String): Either[String, OpenApiOperation] = {
    val paths = Option(openApi.getPaths).getOrElse(Paths()).asScala
    for {
      pathItem <- paths.get(path).toRight(s"No path matching '$path' was found in the OAS document")
      operation <- getOperationForMethod(method, pathItem).toRight(s"No $method operation found for the path '$path' in the OAS document")
    } yield OpenApiOperation(operation, method, path)
  }

  def getApiName()(implicit messagesProvider: MessagesProvider): Either[String, String] =
    Option(openApi.getInfo.getTitle).toRight(Messages.apply("produceApiEnterOas.error.missingApiName"))

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
      case _ => null
    })
  }

  case class OpenApiOperation(operation: Operation, method: String, path: String) {
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

object OpenApiDoc extends Logging {
  private val simpleModule = SimpleModule().addSerializer(JsonNodeExampleSerializer());
  io.swagger.util.Json.mapper().registerModule(simpleModule);

  def parse(openApiSpecification: String)(implicit messagesProvider: MessagesProvider): Either[String, OpenApiDoc] = {
    val options: ParseOptions = new ParseOptions()
    options.setResolve(false)

    val errorMessage = Messages("produceApiEnterOas.error.malformed")
    val parseResult = new OpenAPIV3Parser().readContents(openApiSpecification, null, options)
    parseResult.getOpenAPI match {
      case null => {
        logger.warn((errorMessage :: List(parseResult.getMessages)).mkString(", "))
        Left(errorMessage)
      }
      case openApi => Right(OpenApiDoc(openApi))
    }
  }
}
