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

import base.SpecBase
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.{Parameter, RequestBody}
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem}
import models.{CORPORATE, MDTP}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import play.api.i18n.*

class OpenApiDocSpec extends SpecBase with MockitoSugar {

  private implicit val messagesProvider: MessagesProvider = mock[MessagesProvider]
  private val messages: Messages = mock[Messages]
  private val errorMessage = "Error message"
  when(messagesProvider.messages).thenReturn(messages)
  when(messages.apply(anyString, any)).thenReturn(errorMessage)

  "getServerForApiWorld" - {
    val openApi = OpenAPI()

    val mdtpServer = new Server().description("MDTP - QA")
    val corporateServer = new Server().description("Corporate - Test")
    val otherServer = new Server().description("some other server")

    openApi.addServersItem(mdtpServer)
    openApi.addServersItem(corporateServer)
    openApi.addServersItem(otherServer)

    "must find MDTP server correctly" in {
      OpenApiDoc(openApi).getServerForApiWorld(MDTP) mustBe Some(mdtpServer)
    }

    "must find CORPORATE server correctly" in {
      OpenApiDoc(openApi).getServerForApiWorld(CORPORATE) mustBe Some(corporateServer)
    }

    "must return None if server cannot be found" in {
      OpenApiDoc(OpenAPI()).getServerForApiWorld(CORPORATE) mustBe None
    }
  }

  "getOperation" - {
    val pathItem = new PathItem()
    val path = "/some/path"
    "must return the correct operation for each valid HTTP method" in {
      forAll(Table(
        ("HTTP Method", "call"),
        ("GET", pathItem.setGet),
        ("POST", pathItem.setPost),
        ("PUT", pathItem.setPut),
        ("DELETE", pathItem.setDelete),
        ("OPTIONS", pathItem.setOptions),
        ("HEAD", pathItem.setHead),
        ("PATCH", pathItem.setPatch),
        ("TRACE", pathItem.setTrace)
      )) { (httpMethod, setter) =>
        val openApi = OpenAPI()
        val operation = new Operation()
        setter(operation)
        openApi.path(path, pathItem)

        val openApiDoc = OpenApiDoc(openApi)
        openApiDoc.getOperation(httpMethod, path) mustBe Right(openApiDoc.OpenApiOperation(operation, httpMethod, path))
      }
    }

    "must return an error message for an unknown path" in {
      val openApi = OpenAPI()
      val operation = new Operation()

      val openApiDoc = OpenApiDoc(openApi)
      openApiDoc.getOperation("GET", "/bad/path") mustBe Left("No path matching '/bad/path' was found in the OAS document")
    }

    "must return an error message for an unsupported HTTP method" in {
      val openApi = OpenAPI()
      val operation = new Operation()
      openApi.path(path, pathItem)

      val openApiDoc = OpenApiDoc(openApi)
      openApiDoc.getOperation("DEBUG", path) mustBe Left("No DEBUG operation found for the path '/some/path' in the OAS document")
    }
  }

  "OpenApiOperation" - {
    val operation = Operation()
    operation.addParametersItem(Parameter().in("query").name("queryParam1").schema(StringSchema()))
    operation.addParametersItem(Parameter().in("query").name("queryParam2").schema(IntegerSchema()))
    operation.addParametersItem(Parameter().in("path").name("pathParam1").schema(StringSchema()))
    operation.addParametersItem(Parameter().in("path").name("pathParam2").schema(IntegerSchema()))
    operation.addParametersItem(Parameter().in("header").name("headerParam1").schema(StringSchema()))
    operation.addParametersItem(Parameter().in("header").name("headerParam2").schema(IntegerSchema()))

    def buildOpenApiOperation(operation: Operation, openAPI: OpenAPI = OpenAPI()) = {
      OpenApiDoc(openAPI).OpenApiOperation(operation, "GET", "/some/path")
    }
    "returns query parameters correctly" in {
      buildOpenApiOperation(operation).queryParams mustBe Map("queryParam1" -> "__STRING__", "queryParam2" -> "__INTEGER__")
    }

    "returns path parameters correctly" in {
      buildOpenApiOperation(operation).pathParams mustBe Map("pathParam1" -> "__PATHPARAM1_STRING__", "pathParam2" -> "__PATHPARAM2_INTEGER__")
    }

    "returns header parameters correctly" in {
      buildOpenApiOperation(operation).headerParams mustBe Map("headerParam1" -> "__STRING__", "headerParam2" -> "__INTEGER__")
    }

    "exampleRequestBody" - {
      "must return None if there is no request body" in {
        buildOpenApiOperation(Operation()).exampleRequestBody mustBe None
      }
      "must return None if the request body has no content" in {
        buildOpenApiOperation(Operation().requestBody(RequestBody())).exampleRequestBody mustBe None
      }
      "must return None if the request body content is not JSON" in {
        val content = Content().addMediaType("application/xml", new MediaType())
        val requestBody = RequestBody().content(content)
        buildOpenApiOperation(Operation().requestBody(requestBody)).exampleRequestBody mustBe None
      }
      "must return None if the request body json content has no schema" in {
        val content = Content().addMediaType("application/json", new MediaType())
        val requestBody = RequestBody().content(content)
        buildOpenApiOperation(Operation().requestBody(requestBody)).exampleRequestBody mustBe None
      }
      "must return valid example JSON if the request body json content has a schema" in {
        val schemaRef = "#/components/schemas/Person"
        val openAPI = OpenAPI().schema("Person", new ObjectSchema()
          .$ref(schemaRef)
          .addProperty("name", new StringSchema())
          .addProperty("age", new IntegerSchema()))
        val content = Content().addMediaType("application/json", new MediaType().schema(openAPI.getComponents.getSchemas.get("Person")))
        val requestBody = RequestBody().content(content)
        buildOpenApiOperation(Operation().requestBody(requestBody), openAPI).exampleRequestBody.isDefined mustBe true
      }

    }

    "parse" - {
      "must return a valid result if the provided OAS is correct" in {
        val validOAS =
          """
            |openapi: 3.0.1
            |info:
            |  title: title
            |  description: This is a sample server
            |  license:
            |    name: Apache-2.0
            |    url: http://www.apache.org/licenses/LICENSE-2.0.html
            |  version: 1.0.0
            |servers:
            |- url: https://api.absolute.org/v2
            |  description: An absolute path
            |paths:
            |  /whatever:
            |    get:
            |      summary: Some operation
            |      description: Some operation
            |      operationId: doWhatever
            |      responses:
            |        "200":
            |          description: OK
            |""".stripMargin

        val result = OpenApiDoc.parse(validOAS)

        result mustBe a[Right[?, ?]]
      }
    }
  }
}
