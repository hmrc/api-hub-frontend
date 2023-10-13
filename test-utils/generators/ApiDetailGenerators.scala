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

package generators

import models.api.{ApiDetail, Endpoint, EndpointMethod, IntegrationResponse}
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

trait ApiDetailGenerators {

  private val maxListSize = 10

  implicit lazy val arbitraryEndpointMethod: Arbitrary[EndpointMethod] =
    Arbitrary {
      for {
        httpMethod <- Gen.oneOf("GET", "POST", "PUT", "PATCH", "DELETE")
        summary <- Gen.option(Gen.alphaNumStr)
        description <- Gen.option(Gen.alphaNumStr)
        scopes <- Gen.listOf(Gen.alphaNumStr.suchThat(_.nonEmpty))
      } yield EndpointMethod(httpMethod, summary, description, scopes)
    }

  implicit lazy val arbitraryEndpoint: Arbitrary[Endpoint] =
    Arbitrary {
      for {
        path <- Gen.alphaNumStr
        methods <- Gen.nonEmptyListOf(arbitraryEndpointMethod.arbitrary)
      } yield Endpoint(path, methods)
    }

  implicit lazy val arbitraryApiDetail: Arbitrary[ApiDetail] =
    Arbitrary {
      for {
        id <- Gen.uuid
        title <- Gen.alphaNumStr
        description <- Gen.alphaNumStr
        version <- Gen.alphaNumStr
        endpoints <- Gen.nonEmptyListOf(arbitraryEndpoint.arbitrary)
        shortDescription <- Gen.alphaNumStr
        openApiSpecification <- Gen.alphaNumStr
      } yield ApiDetail(id.toString, title,description, version, endpoints, Some(shortDescription), openApiSpecification)
    }

  implicit val arbitraryApiDetails: Arbitrary[Seq[ApiDetail]] =
    Arbitrary {
      Gen.nonEmptyListOf(arbitraryApiDetail.arbitrary)
    }

  private val parameters = Gen.Parameters.default.withSize(maxListSize)

  def sampleApiDetail(): ApiDetail =
    arbitraryApiDetail.arbitrary.pureApply(parameters, Seed.random())

  def sampleApis() : IntegrationResponse =
    IntegrationResponse(1, None, arbitraryApiDetails.arbitrary.pureApply(parameters, Seed.random()))

  def sampleOas: String =
    """openapi: 3.0.3
      |info:
      |  title: Swagger Sample - OpenAPI 3.0
      |  description: |-
      |    This is a sample
      |  version: 1.0.0
      |tags:
      |  - name: thing
      |    description: put thing
      |paths:
      |  /thing:
      |    put:
      |      tags:
      |        - thing
      |      summary: Update a thing
      |      description: Update an existing pet by Id
      |      operationId: updatePet
      |      requestBody:
      |        description: Update a thing
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Thing'
      |          application/xml:
      |            schema:
      |              $ref: '#/components/schemas/Thing'
      |          application/x-www-form-urlencoded:
      |            schema:
      |              $ref: '#/components/schemas/Thing'
      |        required: true
      |      responses:
      |        '200':
      |          description: Successful operation
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Thing'
      |            application/xml:
      |              schema:
      |                $ref: '#/components/schemas/Thing'
      |        '400':
      |          description: Invalid ID supplied
      |        '404':
      |          description: Pet not found
      |        '405':
      |          description: Validation exception
      |      security:
      |        - petstore_auth:
      |            - write:pets
      |            - read:pets
      |
      |components:
      |  schemas:
      |    Thing:
      |      properties:
      |        id:
      |          type: integer
      |          example: 10
      |      xml:
      |        name: thing
      |  requestBodies:
      |    Thing:
      |      description: Thing that needs to be added
      |      content:
      |        application/json:
      |          schema:
      |            $ref: '#/components/schemas/Thing'
      |        application/xml:
      |          schema:
      |            $ref: '#/components/schemas/Thing'
      |  securitySchemes:
      |    petstore_auth:
      |      type: oauth2
      |      flows:
      |        implicit:
      |          authorizationUrl: https://petstore3.swagger.io/oauth/authorize
      |          scopes:
      |            write:pets: modify pets in your account
      |            read:pets: read your pets
      |""".stripMargin

}
