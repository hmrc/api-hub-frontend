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

import base.SpecBase
import controllers.actions.{FakeApiDetail, FakeApplication}
import io.swagger.v3.oas.models.servers.Server
import models.api.{Endpoint, EndpointMethod}
import models.application.{Credential, Environment, Environments, SelectedEndpoint, Api}
import models.{CORPORATE, CurlCommand, MDTP}
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDateTime

class CurlCommandServiceSpec extends SpecBase with MockitoSugar {
  "CurlCommandService.buildCurlCommandsForApi" - {
    "must return an error if the oas file cannot be parsed" in {
      val service = new CurlCommandService()
      val application = FakeApplication
      val apiDetail = FakeApiDetail.copy(openApiSpecification = "not valid")
      val apiWorld = CORPORATE

      val result = service.buildCurlCommandsForApi(application, apiDetail, apiWorld)

      result mustBe Left("Unable to parse the OAS document")
    }

    "must return the correct curl command for a minimal valid oas file" in {
      val service = new CurlCommandService()
      val application = FakeApplication.copy(apis = Seq(Api("id", "title", Seq(SelectedEndpoint("GET", "/get_it")))))
      val minimalValidOas =
        """
          |openapi: 3.0.3
          |info:
          |  title: Minimal valid OAS
          |  version: 0.1.0
          |servers:
          |- url: http://example.com
          |paths:
          |  /get_it:
          |    get:
          |      description: Gets it
          |      responses:
          |        "200":
          |          description: Successful operation
          |
          |""".stripMargin
      val apiDetail = FakeApiDetail.copy(openApiSpecification = minimalValidOas).copy(endpoints = Seq(Endpoint("/get_it", Seq(EndpointMethod("GET", None, None, Seq.empty)))))
      val apiWorld = CORPORATE

      val result = service.buildCurlCommandsForApi(application, apiDetail, apiWorld)

      result mustBe Right(List(CurlCommand("GET", None, "/get_it", Map.empty, Map.empty,
        Map("Content-Type" -> "application/json"), None)))
    }

    "must return the correct curl commands if the oas file can be parsed" in {
      val service = new CurlCommandService()
      val credential = Credential("client-id", LocalDateTime.now, Some("client-secret"), None)
      val validOas =
        """
          |---
          |openapi: 3.0.3
          |info:
          |  title: HIP Board Game Store
          |  version: 0.1.0
          |servers:
          |- url: http://example.com/api
          |  description: MDTP - QA
          |paths:
          |  /findByColour:
          |    get:
          |      description: request with query parameter
          |      parameters:
          |      - name: colour
          |        in: query
          |        schema:
          |          type: string
          |      responses:
          |        "200":
          |          description: successful operation
          |  /thing/{id}:
          |    get:
          |      description: request with path parameter
          |      parameters:
          |      - name: id
          |        in: path
          |        required: true
          |        schema:
          |          type: string
          |      responses:
          |        "200":
          |          description: successful operation
          |    delete:
          |      description: request that shouldn't generate a curl command
          |      responses:
          |        "200":
          |          description: successful operation
          |  /thing:
          |    delete:
          |      description: request with header parameter
          |      parameters:
          |      - name: thingName
          |        in: header
          |        description: The name of the thing to be deleted
          |        required: true
          |        schema:
          |          type: string
          |      responses:
          |        "200":
          |          description: successful operation
          |  /another:
          |    get:
          |      description: another request that shouldn't generate a curl command
          |      responses:
          |        "200":
          |          description: successful operation
          |components:
          |  schemas:
          |    Thing:
          |      required:
          |      - id
          |      type: object
          |      properties:
          |        id:
          |          format: int64
          |          type: integer
          |          example: 123456
          |        colour:
          |          type: string
          |          example: orange
          |        hostile:
          |          type: boolean
          |  examples:
          |    ValidThing:
          |      value: |
          |        {
          |          "id": 10,
          |          "colour": "Red",
          |          "hostile": true
          |        }

          |
          |""".stripMargin
      val apiDetail = FakeApiDetail.copy(openApiSpecification = validOas).copy(endpoints = Seq(
        Endpoint("/findByColour", Seq(EndpointMethod("GET", None, None, Seq.empty))),
        Endpoint("/thing/{id}", Seq(EndpointMethod("GET", None, None, Seq.empty), EndpointMethod("DELETE", None, None, Seq.empty))),
        Endpoint("/thing", Seq(EndpointMethod("DELETE", None, None, Seq.empty))),
        Endpoint("/another", Seq(EndpointMethod("GET", None, None, Seq.empty))),
      ))

      val application = FakeApplication
        .copy(environments = Environments(
          primary = Environment(Seq.empty, Seq.empty),
          secondary = Environment(Seq.empty, Seq(credential))
        ))
        .copy(apis = Seq(
          Api(apiDetail.id, apiDetail.title, Seq(
            SelectedEndpoint("GET", "/findByColour"),
            SelectedEndpoint("GET", "/thing/{id}"),
            SelectedEndpoint("DELETE", "/thing")
          ))
        ))

      val result = service.buildCurlCommandsForApi(application, apiDetail, MDTP)
      val commonHeaders = Map("Content-Type" -> "application/json", "Authorization" -> "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")
      val server = Server().url("http://example.com/api").description("MDTP - QA")

      val c1 = CurlCommand("GET", Some(server), "/findByColour", Map("colour" -> "__STRING__"), Map.empty, commonHeaders, None)
      val c2 = CurlCommand("GET", Some(server), "/thing/{id}", Map.empty, Map("id" -> "__ID_STRING__"), commonHeaders, None)
      val c3 = CurlCommand("DELETE", Some(server), "/thing", Map.empty, Map.empty, commonHeaders + ("thingName" -> "__STRING__"), None)

      result mustBe Right(List(c1, c2, c3))
    }
  }
}