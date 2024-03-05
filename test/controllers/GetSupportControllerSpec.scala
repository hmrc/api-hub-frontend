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

import base.OptionallyAuthenticatedSpecBase
import config.FrontendAppConfig
import controllers.actions.FakeUser
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.HtmlValidation
import views.html.GetSupportView

class GetSupportControllerSpec extends OptionallyAuthenticatedSpecBase with OptionValues with HtmlValidation {

  "Get Support Controller" - {

    "must return OK and the correct view for a GET with an unauthenticated user" in {
      val application = applicationBuilder(None).build()

      running(application) {
        val request = FakeRequest(GET, routes.GetSupportController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GetSupportView]
        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(config.supportEmailAddress, None)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET with an authenticated user" in {
      val application = applicationBuilder(Some(FakeUser)).build()

      running(application) {
        val request = FakeRequest(GET, routes.GetSupportController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GetSupportView]
        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(config.supportEmailAddress, Some(FakeUser))(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }
}
