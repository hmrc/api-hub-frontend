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
import forms.myapis.produce.ProduceApiReviewNameDescriptionFormProvider
import models.myapis.produce.ProduceApiReviewNameDescription
import models.myapis.produce.ProduceApiReviewNameDescription.Confirm
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.update.{UpdateApiEnterApiTitlePage, UpdateApiEnterOasPage, UpdateApiReviewNameDescriptionPage, UpdateApiShortDescriptionPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import viewmodels.myapis.produce.ProduceApiReviewNameDescriptionViewModel
import views.html.myapis.produce.ProduceApiReviewNameDescriptionView

import scala.concurrent.Future

class UpdateApiReviewNameDescriptionControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ProduceApiReviewNameDescriptionFormProvider()
  private val form = formProvider()
  private def viewModel(mode: Mode) =
    ProduceApiReviewNameDescriptionViewModel(
      controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onSubmit(mode)
    )
  private val apiName = "API NAME"
  private val apiDescription = "API Description"
  private val userAnswersWithDescription = UserAnswers(userAnswersId)
    .set(UpdateApiShortDescriptionPage, apiDescription).success.value
    .set(UpdateApiEnterOasPage, "oas").success.value
    .set(UpdateApiEnterApiTitlePage, apiName).success.value
  private val userAnswersWithNoApiName = UserAnswers(userAnswersId)
    .set(UpdateApiShortDescriptionPage, apiDescription).success.value
    .set(UpdateApiEnterOasPage, "oas").success.value
  private val userAnswersWithPreviousConfirmation = userAnswersWithDescription.set(UpdateApiReviewNameDescriptionPage, Set(Confirm)).success.value

  "UpdateApiReviewNameDescription Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture(Some(userAnswersWithDescription))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode).url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiReviewNameDescriptionView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, apiName, apiDescription, FakeUser, viewModel(NormalMode))(request, messages(fixture.application)).toString
      }
    }

    "must NOT populate the view on a GET even when the question has previously been answered" in {
      val fixture = buildFixture(Some(userAnswersWithPreviousConfirmation))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(CheckMode).url)

        val view = fixture.application.injector.instanceOf[ProduceApiReviewNameDescriptionView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, CheckMode, apiName, apiDescription, FakeUser, viewModel(CheckMode))(request, messages(fixture.application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(Some(userAnswersWithPreviousConfirmation))

      running(fixture.application) {
        val request =
          FakeRequest(POST, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode).url)
            .withFormUrlEncodedBody(("value[0]", ProduceApiReviewNameDescription.values.head.toString))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(Some(userAnswersWithDescription))

      running(fixture.application) {
        val request =
          FakeRequest(POST, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode).url)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = fixture.application.injector.instanceOf[ProduceApiReviewNameDescriptionView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, apiName, apiDescription, FakeUser, viewModel(NormalMode))(request, messages(fixture.application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode).url)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no API data is found" in {
      val fixture = buildFixture(Some(userAnswersWithNoApiName))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode).url)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode).url)
            .withFormUrlEncodedBody(("value[0]", ProduceApiReviewNameDescription.values.head.toString))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(application: PlayApplication, sessionRepository: ProduceApiSessionRepository)

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val sessionRepository = mock[ProduceApiSessionRepository]
    when(sessionRepository.set(any())).thenReturn(Future.successful(true))

    val playApplication = applicationBuilder(userAnswers)
      .overrides(
        bind[ProduceApiSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

    Fixture(playApplication, sessionRepository)
  }
}
