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

import config.HipEnvironment
import fakes.FakeHipEnvironments
import models.application.*
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

import java.time.{Instant, LocalDateTime, ZoneId}

trait ApplicationGenerator {

  val appIdGenerator: Gen[String] = {
    Gen.listOfN(24, Gen.hexChar).map(_.mkString.toLowerCase)
  }

  val localDateTimeGenerator: Gen[LocalDateTime] = {
    for {
      calendar <- Gen.calendar
    } yield LocalDateTime.ofInstant(Instant.ofEpochMilli(calendar.getTimeInMillis), ZoneId.of("GMT"))
  }

  val emailGenerator: Gen[String] = {
    for {
      username <- Gen.alphaLowerStr
      emailDomain <- Gen.oneOf(Seq("@hmrc.gov.uk", "@digital.hmrc.gov.uk"))
    } yield s"$username$emailDomain"
  }

  val creatorGenerator: Gen[Creator] = {
    for {
      email <- emailGenerator
    } yield Creator(email)
  }

  val teamIdGenerator: Gen[Option[String]] = {
    for {
      teamId <- Gen.option(Gen.uuid)
    } yield teamId.map(_.toString)
  }

  val teamMemberGenerator: Gen[TeamMember] = {
    for {
      email <- emailGenerator
    } yield TeamMember(email)
  }

  val hipEnvironmentGenerator: Gen[HipEnvironment] = {
    Gen.oneOf(FakeHipEnvironments.environmentsConfig)
  }

  val credentialGenerator: Gen[Credential] = {
    for {
      clientId <- Gen.uuid
      clientSecret <- Gen.uuid
      created <- localDateTimeGenerator
      hipEnvironment <- hipEnvironmentGenerator
    } yield Credential(clientId.toString, created, Some(clientSecret.toString), Some(clientSecret.toString.takeRight(4)), hipEnvironment.id)
  }

  private def applicationGen: Gen[Application] = Gen.sized { _ =>
    for {
      appId <- appIdGenerator
      name <- Gen.alphaStr
      created <- localDateTimeGenerator
      createdBy <- creatorGenerator
      lastUpdated <- localDateTimeGenerator
      teamId <- teamIdGenerator
      teamMembers <- Gen.listOf(teamMemberGenerator)
      credentials <- Gen.listOf(credentialGenerator)
    } yield
      Application(
        appId,
        name,
        created,
        createdBy,
        lastUpdated,
        teamId,
        teamMembers,
        Seq.empty,
        credentials = credentials.toSet
      )
  }

  implicit val applicationGenerator: Arbitrary[Application] = Arbitrary(applicationGen)

  implicit val newApplicationGenerator: Arbitrary[NewApplication] =
    Arbitrary {
      for {
        name <- Gen.alphaStr
        createdBy <- creatorGenerator
        teamMembers <- Gen.listOf(teamMemberGenerator)
      } yield
        NewApplication(
          name,
          createdBy,
          teamMembers
        )
    }

  private val parameters = Gen.Parameters.default

  def sampleApplication(): Application =
    applicationGen.pureApply(parameters, Seed.random())

}
