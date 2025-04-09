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
import forms.{CreateTeamNameFormProvider, YesNoFormProvider}
import models.team.Team
import models.{NormalMode, CheckMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CreateTeamApiProducerConsumerPage, CreateTeamNamePage}
import play.api.Application
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.team.{CreateTeamNameView, SetTeamApiProducerView}

import java.time.LocalDateTime
import scala.concurrent.Future

class ManageTeamProducerConsumerControllerSpec extends SpecBase with MockitoSugar with HtmlValidation{

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new YesNoFormProvider()
  private val form = formProvider("manageTeam.apiProducer.error")

  private lazy val createTeamProducerConsumerRoute = team.routes.ManageTeamProducerConsumerController.onPageLoad(NormalMode).url
  private lazy val changeTeamProducerConsumerRoute = team.routes.ManageTeamProducerConsumerController.onPageLoad(CheckMode).url


  "ManageTeamProducerConsumer Controller" - {

    "must return OK and the correct view for a GET" in {
      val name = "test-team-name"
      val userAnswers = UserAnswers(userAnswersId).set(CreateTeamNamePage, name).success.value
      val fixture = buildFixture(Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, createTeamProducerConsumerRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[SetTeamApiProducerView]

        redirectLocation(result) mustBe None
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET when the question has been previously answered" in {
      val name = "test-team-name"
      val userAnswers = UserAnswers(userAnswersId)
        .set(CreateTeamNamePage, name).success.value
        .set(CreateTeamApiProducerConsumerPage, true).success.value
      val fixture = buildFixture(Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, changeTeamProducerConsumerRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[SetTeamApiProducerView]

        redirectLocation(result) mustBe None
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), CheckMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }


    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(Some(emptyUserAnswers))

      when(fixture.sessionRepository.set(any)).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamProducerConsumerRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(Some(emptyUserAnswers))

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamProducerConsumerRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = fixture.application.injector.instanceOf[SetTeamApiProducerView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, createTeamProducerConsumerRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, createTeamProducerConsumerRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    application: Application,
    sessionRepository: SessionRepository
  )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val sessionRepository = mock[SessionRepository]

    val application = applicationBuilder(userAnswers)
      .overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

    Fixture(application, sessionRepository)
  }

}
