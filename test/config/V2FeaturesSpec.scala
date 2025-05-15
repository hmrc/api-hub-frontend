/*
 * Copyright 2025 HM Revenue & Customs
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

package config

import models.team.Team
import models.team.TeamType.ConsumerTeam
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDateTime

class V2FeaturesSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  import V2FeaturesSpec.*

  "userAccess" - {
    "must return true when the v2 feature flag is enabled" in {
      val config = mock[FrontendAppConfig]

      when(config.showApisOnDashboard).thenReturn(true)

      V2Features.userAccess(Seq.empty, config) mustBe true
    }

    "must return true when the user is a member of a v2 enabled team" in {
      val config = mock[FrontendAppConfig]
      val team1 = buildTeam(1)
      val team2 = buildTeam(2)
      val team3 = buildTeam(3)

      when(config.showApisOnDashboard).thenReturn(false)
      when(config.v2EnabledTeams).thenReturn(Set(team1.id, team2.id))

      V2Features.userAccess(Seq(team2, team3), config) mustBe true
    }

    "must return false when the user is not a member of a v2 enabled team" in {
      val config = mock[FrontendAppConfig]
      val team1 = buildTeam(1)
      val team2 = buildTeam(2)
      val team3 = buildTeam(3)

      when(config.showApisOnDashboard).thenReturn(false)
      when(config.v2EnabledTeams).thenReturn(Set(team1.id, team2.id))

      V2Features.userAccess(Seq(team3), config) mustBe false
    }
  }

  "teamAccess" - {
    "must return true when the v2 feature flag is enabled" in {
      val config = mock[FrontendAppConfig]

      when(config.showApisOnDashboard).thenReturn(true)

      V2Features.teamAccess(buildTeam(1), config) mustBe true
    }

    "must return true when the team is v2 enabled" in {
      val config = mock[FrontendAppConfig]
      val team1 = buildTeam(1)
      val team2 = buildTeam(2)

      when(config.showApisOnDashboard).thenReturn(false)
      when(config.v2EnabledTeams).thenReturn(Set(team1.id, team2.id))

      V2Features.teamAccess(team1, config) mustBe true
    }

    "must return false when the team is not v2 enabled" in {
      val config = mock[FrontendAppConfig]
      val team1 = buildTeam(1)
      val team2 = buildTeam(2)
      val team3 = buildTeam(3)

      when(config.showApisOnDashboard).thenReturn(false)
      when(config.v2EnabledTeams).thenReturn(Set(team1.id, team2.id))

      V2Features.teamAccess(team3, config) mustBe false
    }
  }

  "v2EnabledTeams" - {
    "must return all teams when the v2 feature flag is enabled" in {
      val config = mock[FrontendAppConfig]
      val team1 = buildTeam(1)
      val team2 = buildTeam(2)

      when(config.showApisOnDashboard).thenReturn(true)

      V2Features.v2EnabledTeams(Seq(team1, team2), config) must contain theSameElementsAs Seq(team1, team2)
    }

    "must return only v2 enabled teams" in {
      val config = mock[FrontendAppConfig]
      val team1 = buildTeam(1)
      val team2 = buildTeam(2)
      val team3 = buildTeam(3)

      when(config.showApisOnDashboard).thenReturn(false)
      when(config.v2EnabledTeams).thenReturn(Set(team1.id, team2.id))

      V2Features.v2EnabledTeams(Seq(team2, team3), config) must contain theSameElementsAs Seq(team2)
    }
  }
}

private object V2FeaturesSpec {

  def buildTeam(index: Int): Team = {
    Team(
      id = s"test-team-id-$index",
      name = s"test-team-name-$index",
      created = LocalDateTime.now(),
      teamMembers = Seq.empty,
      teamType = ConsumerTeam,
      egresses = Seq.empty
    )
  }

}
