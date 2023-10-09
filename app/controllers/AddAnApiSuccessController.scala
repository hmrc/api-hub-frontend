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
import controllers.helpers.ErrorResultBuilder
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddAnApiSuccessView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddAnApiSuccessController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           identify: IdentifierAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: AddAnApiSuccessView,
                                           apiHubService: ApiHubService,
                                           errorResultBuilder: ErrorResultBuilder)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(applicationId: String, apiId: String): Action[AnyContent] = identify.async {
    implicit request =>

      for {
        api <- apiHubService.getApiDetail(apiId)
        application <- apiHubService.getApplication(applicationId, false)
      } yield (api, application) match {
        case (Some(apiDetail), Some(application)) => Ok(view(application, apiDetail, Some(request.user)))
        case (Some(_), None) => errorResultBuilder.notFound(
          Messages("site.applicationNotFoundHeading"),
          Messages("site.applicationNotFoundMessage", applicationId))
        case (None, Some(_)) =>
          errorResultBuilder.notFound(
            Messages("site.apiNotFound.heading"),
            Messages("site.apiNotFound.message", apiId))
        case (None, None) =>
          errorResultBuilder.notFound(
            Messages("site.neitherApiNorApplicationFound.heading"),
            Messages("site.neitherApiNorApplicationFound.message", apiId, applicationId))
      }
  }
}
