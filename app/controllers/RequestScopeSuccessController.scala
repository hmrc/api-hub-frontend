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

import controllers.actions._

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RequestScopeSuccessView

import scala.concurrent.ExecutionContext

class RequestScopeSuccessController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: RequestScopeSuccessView,
  apiHubService: ApiHubService
) (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = identify.async {
    implicit request =>
      apiHubService.getApplication(id) map {
        case Some(application) => Ok(view(application))
        case _ => NotFound
      }
  }

}