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
      } yield ApiDetail(id.toString, title,description, version, endpoints, Some(shortDescription))
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

}
