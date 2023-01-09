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

import gkauth.controllers.GatekeeperBaseController
import gkauth.controllers.actions.{GatekeeperAuthorisationActions, GatekeeperStrideAuthorisationActions}
import gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import gkauth.utils.GatekeeperAuthorisationHelper
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.IndexView

import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class StrideHippBaseController @Inject()(
  strideAuthorisationService: StrideAuthorisationService,
  mcc: MessagesControllerComponents,
  view: IndexView,
  val ldapAuthorisationService: LdapAuthorisationService
)(override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthorisationService, mcc)
                                      with GatekeeperStrideAuthorisationActions
                                      with GatekeeperAuthorisationActions
                                      with GatekeeperAuthorisationHelper
                                      with WithUnsafeDefaultFormBinding{


  def showPage: Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>

    Future(Ok("!!!"))
  }

}
