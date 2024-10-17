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

package controllers.myapis.produce

import base.SpecBase
import controllers.actions.FakeUser
import forms.myapis.produce.ProduceApiChooseTeamFormProvider
import generators.TeamGenerator
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.ProduceApiChooseTeamPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.myapis.produce.ProduceApiChooseTeamView

import scala.concurrent.Future

class ProduceApiChooseTeamControllerSpec extends SpecBase with MockitoSugar with TeamGenerator with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ProduceApiChooseTeamFormProvider()
  private val form = formProvider()

  private lazy val produceApiChooseTeamRoute = routes.ProduceApiChooseTeamController.onPageLoad(NormalMode).url

  "ProduceApiChooseTeam Controller" - {

    "must return OK and the correct view for a GET" in {

      val teams = sampleTeams()
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(teams.head)))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiChooseTeamRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiChooseTeamView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, teams.sortBy(_.name.toLowerCase), FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val teams = sampleTeams()
      val team = teams.head

      val userAnswers = UserAnswers(userAnswersId).set(ProduceApiChooseTeamPage, teams.head).success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiChooseTeamRoute)

        val view = fixture.application.injector.instanceOf[ProduceApiChooseTeamView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(team.id), NormalMode, teams.sortBy(_.name.toLowerCase), FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val teams = sampleTeams()
      val team = teams.head

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseTeamRoute)
            .withFormUrlEncodedBody(("value", team.id))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(fixture.apiHubService).findTeamById(eqTo(team.id))(any)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))

      val teams = sampleTeams()
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(teams.head)))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseTeamRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = fixture.application.injector.instanceOf[ProduceApiChooseTeamView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, teams.sortBy(_.name.toLowerCase), FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return Not Found when the selected team does not exist" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      val teamId = "invalid value"

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(None))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseTeamRoute)
            .withFormUrlEncodedBody(("value", teamId))

        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        val result = route(fixture.application, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with ID $teamId."
          )(request, messages(fixture.application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiChooseTeamRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseTeamRoute)
            .withFormUrlEncodedBody(("value", "test-team-id"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              sessionRepository: ProduceApiSessionRepository
                            )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[ProduceApiSessionRepository]

    val playApplication = applicationBuilder(userAnswers)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ProduceApiSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }

}
