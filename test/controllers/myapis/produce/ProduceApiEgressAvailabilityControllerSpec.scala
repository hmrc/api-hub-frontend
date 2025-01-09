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
import controllers.routes
import forms.myapis.produce.ProduceApiEgressAvailabilityFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.{ProduceApiEgressAvailabilityPage, ProduceApiEgressSelectionPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import utils.TestHelpers
import viewmodels.myapis.produce.ProduceApiEgressAvailabilityViewModel
import views.html.myapis.produce.ProduceApiEgressAvailabilityView

import scala.concurrent.Future

class ProduceApiEgressAvailabilityControllerSpec extends SpecBase with MockitoSugar with TestHelpers {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ProduceApiEgressAvailabilityFormProvider()
  val form = formProvider()

  lazy val produceApiEgressAvailabilityRoute = controllers.myapis.produce.routes.ProduceApiEgressAvailabilityController.onPageLoad(NormalMode).url
  val guidesUrl = "http://localhost:8490/guides/integration-hub-guide"

  "ProduceApiEgressAvailability Controller" - {

    "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, produceApiEgressAvailabilityRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ProduceApiEgressAvailabilityView]
          val viewModel = ProduceApiEgressAvailabilityViewModel(guidesUrl, controllers.myapis.produce.routes.ProduceApiEgressAvailabilityController.onSubmit(NormalMode))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, viewModel, FakeUser)(request, messages(application)).toString
        }
      
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = UserAnswers(userAnswersId).set(ProduceApiEgressAvailabilityPage, true).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, produceApiEgressAvailabilityRoute)

          val view = application.injector.instanceOf[ProduceApiEgressAvailabilityView]
          val viewModel = ProduceApiEgressAvailabilityViewModel(guidesUrl, controllers.myapis.produce.routes.ProduceApiEgressAvailabilityController.onSubmit(NormalMode))
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), viewModel, FakeUser)(request, messages(application)).toString
        }
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[ProduceApiSessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[ProduceApiSessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, produceApiEgressAvailabilityRoute).withFormUrlEncodedBody(("value", "true"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must clear previous egress answer if user selects 'No'" in {
      val mockSessionRepository = mock[ProduceApiSessionRepository]
      val userAnswersWithEgress = emptyUserAnswers.set(ProduceApiEgressSelectionPage, "my egress").success.value

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithEgress)).overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[ProduceApiSessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, produceApiEgressAvailabilityRoute).withFormUrlEncodedBody(("value", "false"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(argThat { (userAnswers: UserAnswers) =>
          userAnswers.get(ProduceApiEgressSelectionPage).isEmpty
        })
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, produceApiEgressAvailabilityRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))
        val view = application.injector.instanceOf[ProduceApiEgressAvailabilityView]
        val viewModel = ProduceApiEgressAvailabilityViewModel(guidesUrl, controllers.myapis.produce.routes.ProduceApiEgressAvailabilityController.onSubmit(NormalMode))
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModel, FakeUser)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, produceApiEgressAvailabilityRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiEgressAvailabilityRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
}
