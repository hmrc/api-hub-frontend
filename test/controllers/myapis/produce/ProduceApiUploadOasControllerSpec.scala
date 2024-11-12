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
import config.FrontendAppConfig
import controllers.actions.FakeUser
import controllers.myapis.produce.routes as apiProduceRoutes
import controllers.routes
import forms.myapis.produce.ProduceApiUploadOasFormProvider
import models.myapis.produce.ProduceApiUploadedOasFile
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.{ProduceApiEnterOasPage, ProduceApiUploadOasPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import views.html.myapis.produce.ProduceApiUploadOasView

import scala.concurrent.Future

class ProduceApiUploadOasControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ProduceApiUploadOasFormProvider()
  val form = formProvider()
  val modelData = ProduceApiUploadedOasFile("name", "oas data")

  lazy val produceApiUploadOasControllerRoute = apiProduceRoutes.ProduceApiUploadOasController.onPageLoad(NormalMode).url

  "ProduceApiUploadOasController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiUploadOasControllerRoute)

        val result = route(application, request).value
        val config = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[ProduceApiUploadOasView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, FakeUser, config)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ProduceApiUploadOasPage, modelData).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiUploadOasControllerRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[ProduceApiUploadOasView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(modelData), NormalMode, FakeUser, config)(request, messages(application)).toString
      }
    }

    "must redirect to the next page and update user answers when valid data is submitted" in {
      val mockSessionRepository = mock[ProduceApiSessionRepository]
      val initialUserAnswers = emptyUserAnswers.set(ProduceApiEnterOasPage, "entered oas data").success.value
      val uploadedOasFileData = ProduceApiUploadedOasFile("oas.yaml", "oas data")
      val finalUserAnswers = emptyUserAnswers.set(ProduceApiUploadOasPage, uploadedOasFileData).success.value

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(initialUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[ProduceApiSessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiUploadOasControllerRoute)
            .withFormUrlEncodedBody(
              ("fileName", uploadedOasFileData.fileName),
              ("fileContents", uploadedOasFileData.fileContents)
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository).set(argThat(userAnswers =>
          userAnswers.get(ProduceApiEnterOasPage).isEmpty && userAnswers.get(ProduceApiUploadOasPage).contains(uploadedOasFileData)
        ))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiUploadOasControllerRoute)
            .withFormUrlEncodedBody(
              ("fileName", ""),
              ("fileContents", "")
            )

        val boundForm = form.bind(Map("fileName" -> "", "fileContents" -> ""))
        val config = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[ProduceApiUploadOasView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, FakeUser, config)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, produceApiUploadOasControllerRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiUploadOasControllerRoute)
            .withFormUrlEncodedBody(("value", "yes"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
