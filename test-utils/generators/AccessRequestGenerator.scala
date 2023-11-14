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

import models.accessRequest._
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

import java.time.{LocalDateTime, ZoneId}

trait AccessRequestGenerator {

  private val maxSize = 20
  private val minSize = 5
  private val resizeFactor = 2
  private val maxSensibleStringSize = 100
  private val parameters = Gen.Parameters.default

  private def newSize(size: Int): Int = {
    Math.min(Math.max(size / resizeFactor, minSize), maxSize)
  }

  private def sensiblySizedAlphaNumStr: Gen[String] = Gen.resize(maxSensibleStringSize, Gen.alphaNumStr)

  private def genLocalDateTime: Gen[LocalDateTime] = {
    Gen.calendar.map(calendar => LocalDateTime.ofInstant(calendar.toInstant, ZoneId.systemDefault()))
  }

  private def genAccessRequestStatus: Gen[AccessRequestStatus] = {
    Gen.oneOf(Pending, Approved, Rejected, Cancelled)
  }

  private def genHttpMethod: Gen[String] = {
    Gen.oneOf("GET", "POST", "PUT", "DELETE", "PATCH")
  }

  private def genScopes: Gen[Seq[String]] = Gen.sized {_ =>
    Gen.nonEmptyListOf(sensiblySizedAlphaNumStr)
  }

  private def genAccessRequestEndpoint: Gen[AccessRequestEndpoint] = Gen.sized {size =>
    for {
      httpMethod <- genHttpMethod
      path <- sensiblySizedAlphaNumStr
      scopes <- Gen.resize(newSize(size), genScopes)
    } yield AccessRequestEndpoint(
      httpMethod = httpMethod,
      path = path,
      scopes = scopes
    )
  }

  private def genAccessRequestEndpoints: Gen[Seq[AccessRequestEndpoint]] = Gen.sized {size=>
    Gen.listOf(Gen.resize(size/ resizeFactor, genAccessRequestEndpoint))
  }

  private def genAccessRequestDecision: Gen[AccessRequestDecision] = {
    for {
      decided <- genLocalDateTime
      decidedBy <- sensiblySizedAlphaNumStr
      rejectedReason <- Gen.option(sensiblySizedAlphaNumStr)
    } yield AccessRequestDecision(
      decided = decided,
      decidedBy = decidedBy,
      rejectedReason = rejectedReason
    )
  }

  private def genAccessRequest: Gen[AccessRequest] = Gen.sized {size =>
    for {
      id <- Gen.uuid
      applicationId <- sensiblySizedAlphaNumStr
      apiId <- Gen.uuid
      apiName <- sensiblySizedAlphaNumStr
      status <- genAccessRequestStatus
      endpoints <- Gen.resize(newSize(size), genAccessRequestEndpoints)
      supportingInformation <- sensiblySizedAlphaNumStr
      requested <- genLocalDateTime
      requestedBy <- sensiblySizedAlphaNumStr
      decision <- Gen.option(genAccessRequestDecision)
    } yield AccessRequest(
      id = id.toString,
      applicationId = applicationId,
      apiId = apiId.toString,
      apiName = apiName,
      status = status,
      endpoints = endpoints,
      supportingInformation = supportingInformation,
      requested = requested,
      requestedBy = requestedBy,
      decision = decision
    )
  }

  private def genAccessRequests: Gen[Seq[AccessRequest]] = {
    Gen.nonEmptyListOf(genAccessRequest)
  }

  implicit val arbitraryAccessRequest: Arbitrary[AccessRequest] = Arbitrary(genAccessRequest)

  implicit val arbitraryAccessRequests: Arbitrary[Seq[AccessRequest]] = Arbitrary(genAccessRequests)

  def sampleAccessRequest(): AccessRequest =
    genAccessRequest.pureApply(parameters, Seed.random())

  def sampleAccessRequests(): Seq[AccessRequest] =
    genAccessRequests.pureApply(parameters, Seed.random())

  private def genAccessRequestApi: Gen[AccessRequestApi] = Gen.sized {size =>
    for {
      apiId <- Gen.uuid
      apiName <- sensiblySizedAlphaNumStr
      endpoints <- Gen.resize(newSize(size), genAccessRequestEndpoints)
    } yield AccessRequestApi(
      apiId = apiId.toString,
      apiName = apiName,
      endpoints = endpoints
    )
  }

  private def genAccessRequestApis: Gen[Seq[AccessRequestApi]] = Gen.sized {_ =>
    Gen.listOf(genAccessRequestApi)
  }

  private def genAccessRequestRequest: Gen[AccessRequestRequest] = Gen.sized {size =>
    for {
      applicationId <- Gen.uuid
      supportingInformation <- sensiblySizedAlphaNumStr
      requestedBy <- sensiblySizedAlphaNumStr
      apis <- Gen.resize(newSize(size), genAccessRequestApis)
    } yield AccessRequestRequest(
      applicationId = applicationId.toString,
      supportingInformation = supportingInformation,
      requestedBy = requestedBy,
      apis = apis
    )
  }

  implicit val arbitraryAccessRequestRequest: Arbitrary[AccessRequestRequest] = Arbitrary(genAccessRequestRequest)

  def sampleAccessRequestRequest(): AccessRequestRequest =
    genAccessRequestRequest.pureApply(parameters, Seed.random())

}
