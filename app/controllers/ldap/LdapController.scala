/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.ldap

import models.LdapLogin
import models.LdapLogin.loginForm
import play.api.mvc.MessagesControllerComponents
import services.auth.LdapAuthorisationPredicate
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.LdapUserLoginView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful

@Singleton
class LdapController @Inject()(
                      auth: FrontendAuthComponents,
                      mcc: MessagesControllerComponents,
                      ldapLoginView: LdapUserLoginView,
                   ) extends FrontendController(mcc) {


  def showLogin = Action{ implicit initialRequest =>
    Ok(ldapLoginView(LdapLogin.loginForm))
  }


  def signIn = Action{ implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Ok(ldapLoginView(formWithErrors))
        //Ok("")
      },
      userData => {
        //binding success, you get the actual value.
        val ldapUser = models.LdapLogin(userData.username, userData.userPassword, userData.userEmail)
        Redirect("ldap auth api")
      }
    )}

}
