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

import com.google.inject.{Inject, Singleton}
import controllers.actions.OptionalIdentifierAction
import controllers.helpers.ErrorResultBuilder
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApiDetailsView

import scala.concurrent.ExecutionContext

@Singleton
class ApiDetailsController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  view: ApiDetailsView,
  errorResultBuilder: ErrorResultBuilder,
  optionallyIdentified: OptionalIdentifierAction,
  addAnApiSessionRepository: AddAnApiSessionRepository
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      apiHubService.getApiDetail(id).map {
        case Some(apiDetail) => Ok(view(apiDetail, request.user))
        case _ => errorResultBuilder.notFound(
          Messages("apiDetails.notFound.heading"),
          Messages("apiDetails.notFound.message", id)
        )
      }
  }

}
