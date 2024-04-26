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
import models.api.ApiDetail
import models.requests.OptionalIdentifierRequest
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApiDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDetailsController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  view: ApiDetailsView,
  errorResultBuilder: ErrorResultBuilder,
  optionallyIdentified: OptionalIdentifierAction
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      for {
        maybeApiDetail <- apiHubService.getApiDetail(id)
        result <- maybeApiDetail match {
          case Some(apiDetail) => processApiDetail(apiDetail)
          case None =>
            Future.successful(errorResultBuilder.notFound(
              Messages("site.apiNotFound.heading"),
              Messages("site.apiNotFound.message", id)
            ))
        }
      } yield result
  }

  private def processApiDetail(apiDetail: ApiDetail)(implicit request: OptionalIdentifierRequest[_]) = {
    for {
      maybeApiDeploymentStatuses <- apiHubService.getApiDeploymentStatuses(apiDetail.publisherReference)
      maybeTeamName <- getTeamNameForApi(apiDetail.teamId)
    } yield maybeApiDeploymentStatuses match {
      case Some(apiDeploymentStatuses) =>
        Ok(view(apiDetail, apiDeploymentStatuses, request.user, maybeTeamName))
      case None =>
        errorResultBuilder.internalServerError(s"Unable to retrieve deployment statuses for API ${apiDetail.publisherReference}")
    }
  }

  private def getTeamNameForApi(maybeTeamId: Option[String])(implicit request: OptionalIdentifierRequest[_]) = {
    maybeTeamId match {
      case Some(teamId) => apiHubService.findTeamById(teamId).map(_ match {
        case Some(team) => Some(team.name)
        case None => Some(Messages("apiDetails.details.team.error"))
      })
      case None => Future.successful(None)
    }
  }
}
