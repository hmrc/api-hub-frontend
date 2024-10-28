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

package generators

import models.api.EgressGateway
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

trait EgressGenerator {

  private val maxSensibleStringSize = 50
  private val parameters = Gen.Parameters.default

  private def sensiblySizedAlphaNumStr: Gen[String] = Gen.resize(maxSensibleStringSize, Gen.alphaNumStr)
  
  private def genEgressGateway: Gen[EgressGateway] = Gen.sized { _ =>
    for {
      id <- Gen.uuid
      friendlyName <- sensiblySizedAlphaNumStr
    } yield EgressGateway(
      id = id.toString,
      friendlyName = friendlyName
    )
  }

  private def genEgressGateways: Gen[Seq[EgressGateway]] = {
    Gen.listOfN(5, genEgressGateway)
  }

  def sampleEgressGateways(): Seq[EgressGateway] =
    genEgressGateways.pureApply(parameters, Seed.random())

}
