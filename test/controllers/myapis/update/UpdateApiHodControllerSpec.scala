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
import config.Hods
import controllers.actions.FakeUser
import controllers.routes
import fakes.FakeHods
import forms.myapis.produce.ProduceApiHodFormProvider
import models.api.Hod
import models.{Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.update.UpdateApiHodPage
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.myapis.produce.ProduceApiHodViewModel
import views.html.myapis.produce.ProduceApiHodView

import scala.concurrent.Future

class UpdateApiHodControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private lazy val updateApiHodRoute = controllers.myapis.update.routes.UpdateApiHodController.onPageLoad(NormalMode).url

  private def viewModel(mode: Mode) =
    ProduceApiHodViewModel(
      "updateApiHod.title",
      controllers.myapis.update.routes.UpdateApiHodController.onSubmit(mode)
    )

  private def formWithHods(application: Application) = {
    val hods = application.injector.instanceOf[Hods]
    val formProvider = new ProduceApiHodFormProvider(hods)
    (hods, formProvider())
  }

  "UpdateApiHod Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, updateApiHodRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProduceApiHodView]
        val (hods, form) = formWithHods(application)

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, FakeUser, hods, viewModel(NormalMode))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(UpdateApiHodPage, FakeHods.hods.map(_.code).toSet).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val (hods, form) = formWithHods(application)
        val request = FakeRequest(GET, updateApiHodRoute)

        val view = application.injector.instanceOf[ProduceApiHodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(hods.hods.map(_.code).toSet), FakeUser, hods, viewModel(NormalMode))(request, messages(application)).toString
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
        val (hods, form) = formWithHods(application)
        val request =
          FakeRequest(POST, updateApiHodRoute)
            .withFormUrlEncodedBody(("value[0]", hods.hods.head.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val (hods, form) = formWithHods(application)
        val request =
          FakeRequest(POST, updateApiHodRoute)
            .withFormUrlEncodedBody(("value[0]", "invalid value"))

        val boundForm = form.bind(Map("value[0]" -> "invalid value"))

        val view = application.injector.instanceOf[ProduceApiHodView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, FakeUser, hods, viewModel(NormalMode))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val (hods, form) = formWithHods(application)
        val request = FakeRequest(GET, updateApiHodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val (hods, form) = formWithHods(application)
        val request =
          FakeRequest(POST, updateApiHodRoute)
            .withFormUrlEncodedBody(("value[0]", hods.hods.head.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
