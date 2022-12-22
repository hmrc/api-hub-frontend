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

package controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView
import login.controllers.actions.LdapAuthorisationActions
import login.domain.models.LoggedInUser
import login.services.LdapAuthorisationService
import login.utils.AuthorisationHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 view: IndexView,
                                 val ldapAuthorisationService: LdapAuthorisationService
                               )(implicit override val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AuthorisationHelper with LdapAuthorisationActions {

  def onPageLoad: Action[AnyContent] = anyAuthenticatedUserAction { implicit loggedInRequest =>
    println("LAURA")
    val loggedInUser = implicitly[LoggedInUser]
    println(loggedInUser)
    Future.apply(Ok(view()))
  }
}
