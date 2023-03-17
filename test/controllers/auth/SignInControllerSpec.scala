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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.auth.SignInView

import java.net.URLEncoder

class SignInControllerSpec extends SpecBase with MockitoSugar {

  "Sign In controller" - {
    "must display the correct view for a GET" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(GET, routes.SignInController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[SignInView]
        val config = application.injector.instanceOf[FrontendAppConfig]
        val ldapSignInUrl = s"${config.loginWithLdapUrl}?continue_url=${urlEncode(config.loginContinueUrl)}"
        val strideSignInUrl = s"${config.loginWithStrideUrl}?successURL=${urlEncode(config.loginContinueUrl)}&origin=${config.appName}"

        status(result) mustBe OK
        contentAsString(result) mustBe view(ldapSignInUrl, strideSignInUrl)(request, messages(application)).toString()
      }
    }
  }

  private def urlEncode(s: String): String = {
    URLEncoder.encode(s, "utf-8")
  }

}
