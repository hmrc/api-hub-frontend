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

package controllers.myapis.update

import base.SpecBase
import connectors.ApplicationsConnector
import controllers.actions.FakeUser
import controllers.routes
import forms.myapis.produce.ProduceApiEnterWiremockFormProvider
import models.deployment.{Error, FailuresResponse}
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.update.UpdateApiEnterWiremockPage
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UpdateApiSessionRepository
import viewmodels.myapis.produce.ProduceApiEnterWiremockViewModel
import views.html.myapis.produce.ProduceApiEnterWiremockView

import scala.concurrent.Future

class UpdateApiEnterWiremockControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ProduceApiEnterWiremockFormProvider()
  private val form = formProvider()

  private val validWiremock =
    """
      |mappings:
      |  boardgames-delete-notfound.json: >
      |    {
      |      "request": {
      |        "method": "DELETE",
      |        "urlPattern": "/backend/boardgames/[0-9]+"
      |      },
      |      "response": {
      |        "status": 404,
      |        "bodyFileName": "boardgame-response.json",
      |        "headers": {
      |          "Content-Type": "application/json"
      |        }
      |      }
      |    }
      |files:
      |  boardgame-response.json: >
      |    {
      |      "id": 1,
      |      "name": "Exploding Kittens",
      |      "category": {       
      |        "id": 545,
      |        "name": "Card Games"
      |      },
      |      "photoUrls": [       
      |        "string"
      |      ],
      |      "tags": [
      |        {
      |          "id": 1,
      |          "name": "Most Popular"
      |        }
      |      ],
      |      "status": "available"
      |    }
      |""".stripMargin
    
  private lazy val updateApiEnterWiremockRoute = controllers.myapis.update.routes.UpdateApiEnterWiremockController.onPageLoad(NormalMode).url

  "UpdateApiEnterWiremock Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEnterWiremockRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEnterWiremockView]
        val viewModel = ProduceApiEnterWiremockViewModel(
          controllers.myapis.update.routes.UpdateApiEnterWiremockController.onSubmit(NormalMode), false
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, FakeUser, viewModel)(request, messages(fixture.application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(UpdateApiEnterWiremockPage, "wiremock").success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEnterWiremockRoute)

        val view = fixture.application.injector.instanceOf[ProduceApiEnterWiremockView]
        val viewModel = ProduceApiEnterWiremockViewModel(
          controllers.myapis.update.routes.UpdateApiEnterWiremockController.onSubmit(NormalMode), false
        )
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("wiremock"), FakeUser, viewModel)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request =
          FakeRequest(POST, updateApiEnterWiremockRoute)
            .withFormUrlEncodedBody(("value", validWiremock))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
    
    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEnterWiremockRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, updateApiEnterWiremockRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(application: Application, applicationsConnector: ApplicationsConnector, sessionRepository: UpdateApiSessionRepository)

  private def buildFixture(userAnswers: Option[UserAnswers] = None): Fixture = {
    val applicationsConnector = mock[ApplicationsConnector]
    val sessionRepository = mock[UpdateApiSessionRepository]
    val application = applicationBuilder(userAnswers)
      .overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[ApplicationsConnector].toInstance(applicationsConnector),
        bind[UpdateApiSessionRepository].toInstance(sessionRepository)
      )
      .build()

    Fixture(application, applicationsConnector, sessionRepository)
  }
}
