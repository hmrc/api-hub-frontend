/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import base.SpecBase
import controllers.actions.FakeUser
import models.requests.IdentifierRequest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.HtmlValidation
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}

class JourneyRecoveryControllerSpec extends SpecBase with HtmlValidation {

  "JourneyRecovery Controller" - {

    "when a relative continue Url is supplied" - {

      "must return OK and the continue view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("/foo")
          val request     = IdentifierRequest(FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url), FakeUser)

          val result = route(application, request).value

          val continueView = application.injector.instanceOf[JourneyRecoveryContinueView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual continueView(continueUrl.unsafeValue, request.maybeUser)(request, messages(application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "when an absolute continue Url is supplied" - {

      "must return OK and the start again view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("https://foo.com")
          val request     = IdentifierRequest(FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url), FakeUser)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView(request.maybeUser)(request, messages(application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "when no continue Url is supplied" - {

      "must return OK and the start again view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = IdentifierRequest(FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url), FakeUser)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView(request.maybeUser)(request, messages(application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }
  }
}
