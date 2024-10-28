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
import models.application.TeamMember
import models.team.Team
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

import java.time.{LocalDateTime, ZoneId}

trait TeamGenerator {

  private val maxSensibleStringSize = 100
  private val parameters = Gen.Parameters.default

  private def sensiblySizedAlphaNumStr: Gen[String] = Gen.resize(maxSensibleStringSize, Gen.alphaNumStr)

  private def genLocalDateTime: Gen[LocalDateTime] = {
    Gen.calendar.map(calendar => LocalDateTime.ofInstant(calendar.toInstant, ZoneId.systemDefault()))
  }

  val genEmail: Gen[String] = {
    for {
      username <- sensiblySizedAlphaNumStr
      emailDomain <- Gen.oneOf(Seq("@hmrc.gov.uk", "@digital.hmrc.gov.uk"))
    } yield s"${username.toLowerCase}$emailDomain"
  }

  val genTeamMember: Gen[TeamMember] = {
    for {
      email <- genEmail
    } yield TeamMember(email)
  }

  val genTeamMembers: Gen[Seq[TeamMember]] = Gen.sized {_ =>
    Gen.nonEmptyListOf(genTeamMember)
  }

  private def genTeam: Gen[Team] = Gen.sized {_ =>
    for {
      id <- Gen.uuid
      name <- sensiblySizedAlphaNumStr
      created <- genLocalDateTime
      teamMembers <- genTeamMembers
    } yield Team(
      id = id.toString,
      name = name,
      created = created,
      teamMembers = teamMembers
    )
  }

  private def genTeams: Gen[Seq[Team]] = {
    Gen.nonEmptyListOf(genTeam)
  }

  implicit val arbitraryTeam: Arbitrary[Team] = Arbitrary(genTeam)

  implicit val arbitraryTeams: Arbitrary[Seq[Team]] = Arbitrary(genTeams)

  def sampleTeam(): Team =
    genTeam.pureApply(parameters, Seed.random())

  def sampleTeams(): Seq[Team] =
    genTeams.pureApply(parameters, Seed.random())

}
