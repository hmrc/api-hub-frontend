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
import forms.myapis.produce.ProduceApiEnterOasFormProvider
import models.deployment.{Error, FailuresResponse, InvalidOasResponse}
import models.myapis.produce.ProduceApiUploadedOasFile
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.update.{UpdateApiEnterOasPage, UpdateApiUploadOasPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import views.html.myapis.produce.ProduceApiEnterOasView

import scala.concurrent.Future

class UpdateApiEnterOasControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ProduceApiEnterOasFormProvider()
  private val form = formProvider()

  private val validOAS =
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

  private lazy val updateApiEnterOasRoute = controllers.myapis.update.routes.UpdateApiEnterOasController.onPageLoad(NormalMode).url

  "ProduceApiEnterOas Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEnterOasRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(""), NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(UpdateApiEnterOasPage, "oas").success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEnterOasRoute)

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("oas"), NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
      when(fixture.applicationsConnector.validateOAS(eqTo(validOAS))(any, any))
        .thenReturn(Future.successful(Right(())))

      running(fixture.application) {
        val request =
          FakeRequest(POST, updateApiEnterOasRoute)
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
          FakeRequest(POST, updateApiEnterOasRoute)
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
        val request = FakeRequest(GET, updateApiEnterOasRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val fixture = buildFixture(userAnswers = None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, updateApiEnterOasRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET when an OAS file has been uploaded" in {
      val oasModel = ProduceApiUploadedOasFile("name", validOAS)
      val userAnswers = emptyUserAnswers.set(UpdateApiUploadOasPage, oasModel).success.value
      val fixture = buildFixture(userAnswers = Some(userAnswers))

      when(fixture.applicationsConnector.validateOAS(eqTo(oasModel.fileContents))(any, any))
        .thenReturn(Future.successful(Right(())))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiEnterOasController.onPageLoadWithUploadedOas(NormalMode).url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(oasModel.fileContents), NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must return OK and the correct view for a GET when an OAS file has been uploaded but we have made edits" in {
      val oasModel = ProduceApiUploadedOasFile("name", validOAS)
      val validOASEdited = validOAS.replace("1.0.0", "1.0.1")
      val userAnswers = emptyUserAnswers
        .set(UpdateApiUploadOasPage, oasModel).success.value
        .set(UpdateApiEnterOasPage, validOASEdited).success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      when(fixture.applicationsConnector.validateOAS(eqTo(validOASEdited))(any, any))
        .thenReturn(Future.successful(Right(())))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiEnterOasController.onPageLoadWithUploadedOas(NormalMode).url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validOASEdited), NormalMode, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must return BadRequest and the correct view for a GET when an invalid OAS file has been uploaded" in {
      val oasModel = ProduceApiUploadedOasFile("name", "not valid oas")
      val userAnswers = emptyUserAnswers.set(UpdateApiUploadOasPage, oasModel).success.value
      val fixture = buildFixture(userAnswers = Some(userAnswers))
      val invalidResponse = InvalidOasResponse(FailuresResponse("400", "nope", None))

      when(fixture.applicationsConnector.validateOAS(any())(any, any)).thenReturn(Future.successful(Left(invalidResponse)))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiEnterOasController.onPageLoadWithUploadedOas(NormalMode).url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEnterOasView]
        val formWithError = form.bind(Map("value" -> oasModel.fileContents)).withGlobalError(Json.prettyPrint(Json.toJson(invalidResponse)))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(formWithError.fill(oasModel.fileContents), NormalMode, FakeUser)(request, messages(fixture.application)).toString
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
