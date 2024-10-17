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
import forms.myapis.produce.ProduceApiReviewNameDescriptionFormProvider
import models.UserAnswers
import models.myapis.produce.ProduceApiReviewNameDescription
import models.myapis.produce.ProduceApiReviewNameDescription.Confirm
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.{ProduceApiReviewNameDescriptionPage, ProduceApiShortDescriptionPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import views.html.myapis.produce.ProduceApiReviewNameDescriptionView

import scala.concurrent.Future

class ProduceApiReviewNameDescriptionControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val produceApiReviewNameDescriptionRoute = controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onPageLoad().url

  val formProvider = new ProduceApiReviewNameDescriptionFormProvider()
  val form = formProvider()
  val apiName = "API NAME"
  val apiDescription = "API Description"
  val userAnswersWithDescription = UserAnswers(userAnswersId).set(ProduceApiShortDescriptionPage, apiDescription).success.value
  val userAnswersWithPreviousConfirmation = userAnswersWithDescription.set(ProduceApiReviewNameDescriptionPage, Set(Confirm)).success.value

  "ProduceApiReviewNameDescription Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture(Some(userAnswersWithDescription))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiReviewNameDescriptionRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiReviewNameDescriptionView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, apiName, apiDescription)(request, messages(fixture.application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val fixture = buildFixture(Some(userAnswersWithPreviousConfirmation))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiReviewNameDescriptionRoute)

        val view = fixture.application.injector.instanceOf[ProduceApiReviewNameDescriptionView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Set(Confirm)), apiName, apiDescription)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(Some(userAnswersWithPreviousConfirmation))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiReviewNameDescriptionRoute)
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
          FakeRequest(POST, produceApiReviewNameDescriptionRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = fixture.application.injector.instanceOf[ProduceApiReviewNameDescriptionView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, apiName, apiDescription)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiReviewNameDescriptionRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiReviewNameDescriptionRoute)
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
