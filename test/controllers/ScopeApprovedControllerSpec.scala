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
import controllers.actions.{FakeApprover, FakeUser}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.HtmlValidation
import views.html.ScopeApprovedView

class ScopeApprovedControllerSpec extends SpecBase with HtmlValidation {

  "ScopeApproved Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(user = FakeApprover).build()

      running(application) {
        val request = FakeRequest(GET, routes.ScopeApprovedController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ScopeApprovedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(FakeApprover))(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the unauthorised page when the user is not an approver" in {
      val application = applicationBuilder(user = FakeUser).build()

      running(application) {
        val request = FakeRequest(GET, routes.ScopeApprovedController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

}
