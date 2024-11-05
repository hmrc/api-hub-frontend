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
import controllers.routes
import forms.myapis.produce.ProduceApiPassthroughFormProvider
import models.user.UserModel
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.ProduceApiPassthroughPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.myapis.produce.ProduceApiPassthroughView
import utils.TestHelpers
import scala.concurrent.Future

class ProduceApiPassthroughControllerSpec extends SpecBase with MockitoSugar with TestHelpers {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ProduceApiPassthroughFormProvider()
  val form = formProvider()

  lazy val produceApiPassthroughRoute = controllers.myapis.produce.routes.ProduceApiPassthroughController.onPageLoad(NormalMode).url

  "ProduceApiPassthrough Controller" - {

    "must return OK and the correct view for a GET for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          user = user
        ).build()

        running(application) {
          val request = FakeRequest(GET, produceApiPassthroughRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ProduceApiPassthroughView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, user)(request, messages(application)).toString
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val userAnswers = UserAnswers(userAnswersId).set(ProduceApiPassthroughPage, true).success.value
        val application = applicationBuilder(
          userAnswers = Some(userAnswers),
          user = user
        ).build()

        running(application) {
          val request = FakeRequest(GET, produceApiPassthroughRoute)

          val view = application.injector.instanceOf[ProduceApiPassthroughView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode, user)(request, messages(application)).toString
        }
      }
    }

    "must redirect to the next page when valid data is submitted for a support user" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val application =
          applicationBuilder(
            userAnswers = Some(emptyUserAnswers),
            user = user
          ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, produceApiPassthroughRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          user = user
        ).build()

        running(application) {
          val request =
            FakeRequest(POST, produceApiPassthroughRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[ProduceApiPassthroughView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, user)(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val application = applicationBuilder(userAnswers = None, user = user).build()

        running(application) {
          val request = FakeRequest(GET, produceApiPassthroughRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val application = applicationBuilder(userAnswers = None, user = user).build()

        running(application) {
          val request =
            FakeRequest(POST, produceApiPassthroughRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must redirect to the unauthorised page for a non-support user making a GET request" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          user = user
        ).build()

        running(application) {
          val request = FakeRequest(GET, produceApiPassthroughRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ProduceApiPassthroughView]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must redirect to the unauthorised page for a non-support user making a POST request" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          user = user
        ).build()

        running(application) {
          val request = FakeRequest(POST, produceApiPassthroughRoute).withFormUrlEncodedBody(("value", "true"))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }
}
