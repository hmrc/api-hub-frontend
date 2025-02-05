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
import controllers.actions.FakeUser
import controllers.routes
import forms.myapis.produce.ProduceApiHowToCreateFormProvider
import models.api.ApiDetail
import models.api.ApiDetailLensesSpec.sampleApiDetail
import models.myapis.produce.ProduceApiHowToCreate
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.ProduceApiHowToCreatePage
import pages.myapis.update.{UpdateApiApiPage, UpdateApiHowToUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.myapis.{UpdateApiHowToUpdateViewBannerModel, ProduceApiHowToCreateViewModel}
import views.html.myapis.produce.ProduceApiHowToCreateView

import java.util.UUID
import scala.concurrent.Future

class UpdateApiHowToUpdateControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val updateApiHowToUpdateRoute = controllers.myapis.update.routes.UpdateApiHowToUpdateController.onPageLoad(NormalMode).url

  val formProvider = new ProduceApiHowToCreateFormProvider()
  val form = formProvider("myApis.update.howtoupdate.error.required")
  lazy val viewModel = ProduceApiHowToCreateViewModel(
    "myApis.update.howtoupdate.title",
    "myApis.update.howtoupdate.heading",
    Some(UpdateApiHowToUpdateViewBannerModel("myApis.update.howtoupdate.banner.title","myApis.update.howtoupdate.banner.content")),
    "update",
    controllers.myapis.update.routes.UpdateApiHowToUpdateController.onSubmit(NormalMode))

  val apiDetail = sampleApiDetail()

  "UpdateApiHowToUpdate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = emptyUserAnswers.set(UpdateApiApiPage, apiDetail).toOption).build()

      running(application) {
        val request = FakeRequest(GET, updateApiHowToUpdateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProduceApiHowToCreateView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, viewModel, FakeUser)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(UpdateApiApiPage, apiDetail).toOption.value
        .set(UpdateApiHowToUpdatePage, ProduceApiHowToCreate.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, updateApiHowToUpdateRoute)

        val view = application.injector.instanceOf[ProduceApiHowToCreateView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(ProduceApiHowToCreate.values.head), viewModel, FakeUser)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, updateApiHowToUpdateRoute)
            .withFormUrlEncodedBody(("value", ProduceApiHowToCreate.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers.set(UpdateApiApiPage, apiDetail).toOption

      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val request =
          FakeRequest(POST, updateApiHowToUpdateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ProduceApiHowToCreateView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModel, FakeUser)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, updateApiHowToUpdateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no api detail has been set at the start of the journey" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, updateApiHowToUpdateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, updateApiHowToUpdateRoute)
            .withFormUrlEncodedBody(("value", ProduceApiHowToCreate.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
