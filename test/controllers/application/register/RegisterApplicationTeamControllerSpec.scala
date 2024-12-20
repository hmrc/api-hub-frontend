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

package controllers.application.register

import base.SpecBase
import controllers.actions.FakeUser
import controllers.routes
import forms.application.register.RegisterApplicationTeamFormProvider
import generators.TeamGenerator
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.application.register.RegisterApplicationTeamPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.application.register.RegisterApplicationTeamView

import scala.concurrent.Future

class RegisterApplicationTeamControllerSpec extends SpecBase with MockitoSugar with TeamGenerator with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private lazy val registerApplicationTeamRoute = controllers.application.register.routes.RegisterApplicationTeamController.onPageLoad(NormalMode).url

  private val formProvider = new RegisterApplicationTeamFormProvider()
  private val form = formProvider()

  "RegisterApplicationTeam Controller" - {

    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      val teams = sampleTeams()

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, registerApplicationTeamRoute)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[RegisterApplicationTeamView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, teams.sortBy(_.name.toLowerCase), Some(FakeUser))(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
        verify(fixture.apiHubService).findTeams(eqTo(Some(FakeUser.email)))(any)
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val teams = sampleTeams()
      val team = teams.head
      val userAnswers = UserAnswers(userAnswersId).set(RegisterApplicationTeamPage, team).success.value
      val fixture = buildFixture(userAnswers = Some(userAnswers))

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, registerApplicationTeamRoute)

        val view = fixture.playApplication.injector.instanceOf[RegisterApplicationTeamView]

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(team.id), NormalMode, teams.sortBy(_.name.toLowerCase), Some(FakeUser))(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      val team = sampleTeam()

      when(fixture.sessionRepository.set(any)).thenReturn(Future.successful(true))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, registerApplicationTeamRoute)
            .withFormUrlEncodedBody(("value", team.id))

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(fixture.apiHubService).findTeamById(eqTo(team.id))(any)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      val teams = sampleTeams()

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(POST, registerApplicationTeamRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view = fixture.playApplication.injector.instanceOf[RegisterApplicationTeamView]
        val result = route(fixture.playApplication, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, teams.sortBy(_.name.toLowerCase), Some(FakeUser))(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return Not Found when the selected team does not exist" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      val teamId = "invalid value"

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, registerApplicationTeamRoute)
            .withFormUrlEncodedBody(("value", teamId))

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with ID $teamId.",
            Some(FakeUser)
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(userAnswers = None)

      running(fixture.playApplication) {
        val request = FakeRequest(GET, registerApplicationTeamRoute)

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(userAnswers = None)

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, registerApplicationTeamRoute)
            .withFormUrlEncodedBody(("value", "test-team-id"))

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    sessionRepository: SessionRepository
  )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[SessionRepository]

    val playApplication = applicationBuilder(userAnswers)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[SessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }

}
