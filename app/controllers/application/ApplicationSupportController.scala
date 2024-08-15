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

package controllers.application

import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApplicationAuthActionProvider, AuthorisedSupportAction, IdentifierAction}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

@Singleton
class ApplicationSupportController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  applicationAuth: ApplicationAuthActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen isSupport andThen applicationAuth(id, enrich = true, includeDeleted = true)) {
    implicit request =>
      Ok(Json.toJson(request.application))
  }

}
