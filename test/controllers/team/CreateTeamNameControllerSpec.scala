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
import controllers.{routes, team}
import forms.CreateTeamNameFormProvider
import models.team.Team
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.CreateTeamNamePage
import play.api.Application
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.team.CreateTeamNameView

import java.time.LocalDateTime
import scala.concurrent.Future

class CreateTeamNameControllerSpec extends SpecBase with MockitoSugar with HtmlValidation{

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new CreateTeamNameFormProvider()
  private val form = formProvider()

  private lazy val createTeamNameRoute = team.routes.CreateTeamNameController.onPageLoad(NormalMode).url

  "CreateTeamName Controller" - {

    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(Some(emptyUserAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, createTeamNameRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[CreateTeamNameView]

        redirectLocation(result) mustBe None
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered and the name is unique" in {
      val name = "test-team-name"
      val userAnswers = UserAnswers(userAnswersId).set(CreateTeamNamePage, name).success.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.findTeamByName(any)(any)).thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(GET, createTeamNameRoute)

        val view = fixture.application.injector.instanceOf[CreateTeamNameView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(name), NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeamByName(eqTo(name))(any)
      }
    }

    "must return a Bad Request and errors on a GET when the question has previously been answered and the name is not unique" in {
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq.empty)

      val userAnswers = UserAnswers(userAnswersId).set(CreateTeamNamePage, team.name).success.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.findTeamByName(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request = FakeRequest(GET, createTeamNameRoute)

        val view = fixture.application.injector.instanceOf[CreateTeamNameView]

        val result = route(fixture.application, request).value

        val formWithErrors = form.fill(team.name).withError(FormError("value", "createTeamName.error.nameNotUnique"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeamByName(eqTo(team.name))(any)
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(Some(emptyUserAnswers))

      when(fixture.apiHubService.findTeamByName(any)(any)).thenReturn(Future.successful(None))
      when(fixture.sessionRepository.set(any)).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamNameRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(Some(emptyUserAnswers))

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = fixture.application.injector.instanceOf[CreateTeamNameView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return a Bad Request and errors on a POST when the team name is not unique" in {
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq.empty)

      val fixture = buildFixture(Some(emptyUserAnswers))

      when(fixture.apiHubService.findTeamByName(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamNameRoute)
            .withFormUrlEncodedBody(("value", team.name))

        val view = fixture.application.injector.instanceOf[CreateTeamNameView]

        val result = route(fixture.application, request).value

        val formWithErrors = form.fill(team.name).withError(FormError("value", "createTeamName.error.nameNotUnique"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeamByName(eqTo(team.name))(any)
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, createTeamNameRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamNameRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    application: Application,
    sessionRepository: SessionRepository,
    apiHubService: ApiHubService
  )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val sessionRepository = mock[SessionRepository]
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers)
      .overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[SessionRepository].toInstance(sessionRepository),
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, sessionRepository, apiHubService)
  }

}
