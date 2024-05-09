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

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import controllers.auth.SignInController.{ldapSignInUrl, strideSignInUrl}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.auth.SignInView

import java.net.URLEncoder

@Singleton
class SignInController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  view: SignInView
) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = Action {
    implicit request =>
      Ok(view(ldapSignInUrl(config), strideSignInUrl(config), config.helpDocsPath))
  }

}

object SignInController {

  private def ldapSignInUrl(config: FrontendAppConfig): String = {
    s"${config.loginWithLdapUrl}?${urlEncode("continue_url")}=${urlEncode(config.loginContinueUrl)}"
  }

  private def strideSignInUrl(config: FrontendAppConfig): String = {
    val continueUrl = urlEncode(config.loginContinueUrl)
    val origin = urlEncode(config.appName)
    s"${config.loginWithStrideUrl}?successURL=$continueUrl&origin=$origin"
  }

  private def urlEncode(fragment: String): String = {
    URLEncoder.encode(fragment, "utf-8")
  }

}
