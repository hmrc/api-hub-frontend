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
import controllers.myapis.produce.routes as produceApiRoutes
import controllers.routes
import forms.myapis.produce.ProduceApiAddPrefixesFormProvider
import models.api.Alpha
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.ProduceApiAddPrefixesPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import viewmodels.myapis.produce.ProduceApiAddPrefixesViewModel
import views.html.myapis.produce.ProduceApiAddPrefixesView

import scala.concurrent.Future

class ProduceApiAddPrefixesControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private lazy val produceApiAddPrefixesRoute = produceApiRoutes.ProduceApiAddPrefixesController.onPageLoad(NormalMode).url

  private val formProvider = new ProduceApiAddPrefixesFormProvider()
  private val form = formProvider()
  private val guidesUrl = "http://localhost:8490/guides/integration-hub-guide"

  "ProduceApiAddPrefixes Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiAddPrefixesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProduceApiAddPrefixesView]
        val viewModel = ProduceApiAddPrefixesViewModel(produceApiRoutes.ProduceApiAddPrefixesController.onSubmit(NormalMode))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel, guidesUrl, FakeUser)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ProduceApiAddPrefixesPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiAddPrefixesRoute)

        val view = application.injector.instanceOf[ProduceApiAddPrefixesView]
        val viewModel = ProduceApiAddPrefixesViewModel(produceApiRoutes.ProduceApiAddPrefixesController.onSubmit(NormalMode))
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), viewModel, guidesUrl, FakeUser)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockProduceApiSessionRepository = mock[ProduceApiSessionRepository]

      when(mockProduceApiSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[ProduceApiSessionRepository].toInstance(mockProduceApiSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiAddPrefixesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiAddPrefixesRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ProduceApiAddPrefixesView]
        val viewModel = ProduceApiAddPrefixesViewModel(produceApiRoutes.ProduceApiAddPrefixesController.onSubmit(NormalMode))
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModel, guidesUrl, FakeUser)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, produceApiAddPrefixesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiAddPrefixesRoute)
            .withFormUrlEncodedBody(("value", Alpha.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
