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

package controllers.application

import com.google.inject.Inject
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.{ApplicationApiBuilder, ErrorResultBuilder}
import models.application.Application
import models.application.ApplicationLenses._
import models.team.Team
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.ApplicationDetailsView

import scala.concurrent.{ExecutionContext, Future}

class ApplicationDetailsController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: ApplicationDetailsView,
  applicationApiBuilder: ApplicationApiBuilder,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      fetchTeam(request.application).flatMap {
        case Right(team) =>
          applicationApiBuilder.build(request.application).map {
            case Right(applicationApis) =>
              Ok(view(
                request.application.withSortedTeam(),
                Some(applicationApis),
                team,
                Some(request.identifierRequest.user)
              ))
            case Left(_) => Ok(view(
              request.application.withSortedTeam(),
              None,
              team,
              Some(request.identifierRequest.user)
            ))
          }
        case Left(result) => Future.successful(result)
      }
  }

  private def fetchTeam(application: Application)(implicit request: Request[_]): Future[Either[Result, Option[Team]]] = {
    application.teamId match {
      case Some(teamId) => apiHubService.findTeamById(teamId).map {
        case Some(team) => Right(Some(team))
        case None => Left(teamNotFound(teamId))
      }
      case None => Future.successful(Right(None))
    }
  }

  private def teamNotFound(teamId: String)(implicit request: Request[_]): Result = {
    errorResultBuilder.teamNotFound(teamId)
  }

}
