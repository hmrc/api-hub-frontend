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
import connectors.ApplicationsConnector
import controllers.routes
import forms.myapis.produce.ProduceApiEnterOasFormProvider
import models.deployment.{Error, FailuresResponse, InvalidOasResponse}
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.ProduceApiEnterOasPage
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import views.html.myapis.produce.ProduceApiEnterOasView

import scala.concurrent.Future

class ProduceApiEnterOasControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ProduceApiEnterOasFormProvider()
  val form = formProvider()

  lazy val produceApiEnterOasRoute = controllers.myapis.produce.routes.ProduceApiEnterOasController.onPageLoad(NormalMode).url

  "ProduceApiEnterOas Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiEnterOasRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ProduceApiEnterOasPage, "answer").success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiEnterOasRoute)

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val validOAS =
        """
          |openapi: 3.0.1
          |info:
          |  title: title
          |  description: This is a sample server
          |  license:
          |    name: Apache-2.0
          |    url: http://www.apache.org/licenses/LICENSE-2.0.html
          |  version: 1.0.0
          |servers:
          |- url: https://api.absolute.org/v2
          |  description: An absolute path
          |paths:
          |  /whatever:
          |    get:
          |      summary: Some operation
          |      description: Some operation
          |      operationId: doWhatever
          |      responses:
          |        "200":
          |          description: OK
          |""".stripMargin

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
      when(fixture.applicationsConnector.validateOAS(eqTo(validOAS))(any, any))
        .thenReturn(Future.successful(Right(())))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiEnterOasRoute)
            .withFormUrlEncodedBody(("value", validOAS))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val invalidOAS = "invalid oas"
      val errorMessage = "Unable to parse the OAS document, errorMessage"
      val invalidResponse = InvalidOasResponse(FailuresResponse("400", errorMessage, None))
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.applicationsConnector.validateOAS(eqTo(invalidOAS))(any, any))
        .thenReturn(Future.successful(Left(
          invalidResponse
        )))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiEnterOasRoute)
            .withFormUrlEncodedBody(("value", invalidOAS))

        val boundForm = form.bind(Map("value" -> invalidOAS))
          .withGlobalError(Json.prettyPrint(Json.toJson(invalidResponse)))

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiEnterOasRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiEnterOasRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(application: Application, applicationsConnector: ApplicationsConnector, sessionRepository: ProduceApiSessionRepository)

  private def buildFixture(userAnswers: Option[UserAnswers] = None): Fixture = {
    val applicationsConnector = mock[ApplicationsConnector]
    val sessionRepository = mock[ProduceApiSessionRepository]
    val application = applicationBuilder(userAnswers)
      .overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[ApplicationsConnector].toInstance(applicationsConnector),
        bind[ProduceApiSessionRepository].toInstance(sessionRepository)
      )
      .build()

    Fixture(application, applicationsConnector, sessionRepository)
  }
}
