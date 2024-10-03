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

package controllers.application.cancelaccessrequest

import base.SpecBase
import forms.application.cancelaccessrequest.CancelAccessRequestConfirmFormProvider
import generators.AccessRequestGenerator
import models.accessrequest.AccessRequest
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.application.cancelaccessrequest.{CancelAccessRequestConfirmPage, CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.CancelAccessRequestSessionRepository
import utils.{HtmlValidation, UserAnswersSugar}
import views.html.application.cancelaccessrequest.CancelAccessRequestConfirmView

import scala.concurrent.Future

class CancelAccessConfirmControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with UserAnswersSugar with AccessRequestGenerator {

  import CancelAccessConfirmControllerSpec.*

  "CancelAccessRequestConfirm Controller" - {

    "must return OK and the correct view for a GET" in {
      val accessRequests = sampleAccessRequests()
      val apiIds = accessRequests.map(_.apiId).toSet
      val fixture = buildFixture(accessRequests = Some(accessRequests), apis = Some(apiIds))

      running(fixture.application) {
        val request = FakeRequest(GET, cancelAccessConfirmRoute)
        val result = route(fixture.application, request).value

        status(result) mustBe OK

        contentAsString(result) mustBe fixture.view(fixture.form, NormalMode, accessRequests)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val accessRequests = sampleAccessRequests()
      val apiIds = accessRequests.map(_.apiId).toSet
      val fixture = buildFixture(accessRequests = Some(accessRequests), apis = Some(apiIds), accepted = true)

      running(fixture.application) {
        val request = FakeRequest(GET, cancelAccessConfirmRoute)
        val result = route(fixture.application, request).value
        val filledForm = fixture.form.fill(true)

        status(result) mustBe OK
        contentAsString(result) mustBe fixture.view(filledForm, NormalMode, accessRequests)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val accessRequests = sampleAccessRequests()
      val apiIds = accessRequests.map(_.apiId).toSet
      val fixture = buildFixture(accessRequests = Some(accessRequests), apis = Some(apiIds), accepted = true)

      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, cancelAccessConfirmRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        val expectedAnswers = fixture.userAnswers.value.setQuick(CancelAccessRequestConfirmPage, true)
        verify(fixture.sessionRepository).set(ArgumentMatchers.eq(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val accessRequests = sampleAccessRequests()
      val apiIds = accessRequests.map(_.apiId).toSet
      val fixture = buildFixture(accessRequests = Some(accessRequests), apis = Some(apiIds))

      running(fixture.application) {
        val request = FakeRequest(POST, cancelAccessConfirmRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = fixture.form.bind(Map("value" -> "invalid value"))
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe fixture.view(boundForm, NormalMode, accessRequests)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(hasAnswers = false)

      running(fixture.application) {
        val request = FakeRequest(GET, cancelAccessConfirmRoute)

        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if data is found but has no CancelAccessRequestConfirmPage answer" in {
      val fixture = buildFixture(hasAnswers = false)

      running(fixture.application) {
        val request = FakeRequest(GET, cancelAccessConfirmRoute)

        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(hasAnswers = false)

      running(fixture.application) {
        val request =
          FakeRequest(POST, cancelAccessConfirmRoute)
            .withFormUrlEncodedBody(("value[0]", "etc"))

        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if data is found but has no CancelAccessRequestPendingPage answer" in {
      val fixture = buildFixture(apis = Some(Set("an_api_id")))

      running(fixture.application) {
        val request =
          FakeRequest(POST, cancelAccessConfirmRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if data is found but has no CancelAccessRequestSelectApiPage answer" in {
      val fixture = buildFixture(accessRequests = Some(sampleAccessRequests()))

      running(fixture.application) {
        val request =
          FakeRequest(POST, cancelAccessConfirmRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private def buildFixture(
                            hasAnswers: Boolean = true,
                            accessRequests: Option[Seq[AccessRequest]] = None,
                            apis: Option[Set[String]] = None,
                            accepted: Boolean = false
                          ): Fixture = {
    val userAnswers = if (hasAnswers) {
      Some(
        emptyUserAnswers
        .setIfPresent(CancelAccessRequestPendingPage, accessRequests)
        .setIfPresent(CancelAccessRequestSelectApiPage, apis)
        .set(CancelAccessRequestConfirmPage, accepted).get
      )
    }
    else {
      None
    }

    val sessionRepository = mock[CancelAccessRequestSessionRepository]

    val application = applicationBuilder(userAnswers = userAnswers)
      .overrides(
        bind[CancelAccessRequestSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(FakeNavigator(onwardRoute))
      )
      .build()

    val formProvider = new CancelAccessRequestConfirmFormProvider()
    val form = formProvider.apply().fill(accepted)

    val view = application.injector.instanceOf[CancelAccessRequestConfirmView]

    Fixture(userAnswers, sessionRepository, form, view, application)
  }

}

object CancelAccessConfirmControllerSpec {

  private val onwardRoute = Call("GET", "/CancelAccessConfirmControllerSpec")
  private lazy val cancelAccessConfirmRoute = routes.CancelAccessRequestConfirmController.onPageLoad(NormalMode).url

  private case class Fixture(
                              userAnswers: Option[UserAnswers],
                              sessionRepository: CancelAccessRequestSessionRepository,
                              form: Form[Boolean],
                              view: CancelAccessRequestConfirmView,
                              application: Application
                            )

}
