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
import controllers.actions.FakeUser
import models.{CheckMode, UserAnswers}
import models.application.TeamMember
import models.exception.TeamNameNotUniqueException
import models.team.{NewTeam, Team}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.OptionValues
import pages.{CreateTeamMembersPage, CreateTeamNamePage}
import play.api.{Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.team.CreateTeamSuccessView

import java.time.LocalDateTime
import scala.concurrent.Future

class CreateTeamControllerSpec extends SpecBase with MockitoSugar with ArgumentMatchersSugar with HtmlValidation {

  import CreateTeamControllerSpec._

  "onSubmit" - {
    "must create the team, clear user answers, and return the success view" in {
      val fixture = buildFixture(fullAnswers)

      when(fixture.apiHubService.createTeam(any)(any)).thenReturn(Future.successful(Right(team)))
      when(fixture.sessionRepository.clear(any)).thenReturn(Future.successful(true))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.CreateTeamController.onSubmit())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK

        verify(fixture.apiHubService).createTeam(eqTo(NewTeam(teamName, teamMembers)))(any)
        verify(fixture.sessionRepository).clear(FakeUser.userId)

        val view = fixture.playApplication.injector.instanceOf[CreateTeamSuccessView]

        contentAsString(result) mustBe view(team, Some(FakeUser))(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the team name page if there is no team name answer" in {
      val answers = UserAnswers(FakeUser.userId).set(CreateTeamMembersPage, teamMembers).toOption.value

      val fixture = buildFixture(answers)

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.CreateTeamController.onSubmit())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to the team name page if the team name is not unique" in {
      val fixture = buildFixture(fullAnswers)

      when(fixture.apiHubService.createTeam(any)(any))
        .thenReturn(Future.successful(Left(TeamNameNotUniqueException.forName(teamName))))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.CreateTeamController.onSubmit())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode).url

        verifyZeroInteractions(fixture.sessionRepository)
      }
    }

    "must redirect to the journey recovery page if there is no team members answer" in {
      val answers = UserAnswers(FakeUser.userId).set(CreateTeamNamePage, teamName).toOption.value

      val fixture = buildFixture(answers)

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.CreateTeamController.onSubmit())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    sessionRepository: CreateTeamSessionRepository
  )

  private def buildFixture(userAnswers: UserAnswers): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[CreateTeamSessionRepository]

    val playApplication = applicationBuilder(Some(userAnswers))
      .overrides(
        bind[ApiHubService].to(apiHubService),
        bind[CreateTeamSessionRepository].to(sessionRepository)
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }

}

object CreateTeamControllerSpec extends OptionValues {

  private val teamName = "test-team-name"
  private val teamMembers = Seq(TeamMember(FakeUser.email.value))
  private val team = Team("test-team-id", teamName, LocalDateTime.now(), teamMembers)

  private val fullAnswers = UserAnswers(FakeUser.userId)
    .set(CreateTeamNamePage, teamName).toOption.value
    .set(CreateTeamMembersPage, teamMembers).toOption.value

}
