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

package controllers.team

import base.SpecBase
import controllers.actions.*
import models.{CheckMode, UserAnswers}
import models.application.TeamMember
import models.exception.TeamNameNotUniqueException
import models.team.{NewTeam, Team}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import pages.{CreateTeamMembersPage, CreateTeamNamePage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.team.CreateTeamSuccessView

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TeamCreatedControllerSpec extends SpecBase with MockitoSugar with HtmlValidation {

  "onPageLoad" - {
    "must return the success view" in {
      val team = Team("teamId", "teamName", LocalDateTime.now(), List.empty)
      val fixture = buildFixture(team = Some(team))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.TeamCreatedController.onPageLoad(team.id))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK

        val view = fixture.playApplication.injector.instanceOf[CreateTeamSuccessView]

        contentAsString(result) mustBe view(team, Some(FakeSupporter))(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the team not found page if the team does not exists" in {
      val teamId = "TeamNotFound"

      val fixture = buildFixture()

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.TeamCreatedController.onPageLoad(teamId))
        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with ID $teamId.",
            Some(FakeSupporter)
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication)

  private def buildFixture(team: Option[Team] = None): Fixture = {
    val apiHubService = mock[ApiHubService]
    when(apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(team))

    val playApplication = applicationBuilder(None, FakeSupporter)
      .overrides(
        bind[ApiHubService].to(apiHubService)
      )
      .build()

    Fixture(playApplication)
  }

}
