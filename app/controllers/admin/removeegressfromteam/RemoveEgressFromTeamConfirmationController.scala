/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.admin.removeegressfromteam

import cats.data.EitherT
import com.google.inject.Inject
import config.HipEnvironments
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import controllers.helpers.{ErrorResultBuilder, Fetching}
import models.api.EgressGateway
import models.requests.IdentifierRequest
import models.team.Team
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.removeegressfromteam.{RemoveEgressFromTeamConfirmationViewModel, RemoveEgressFromTeamSuccessViewModel}
import views.html.admin.removeegressfromteam.{RemoveEgressFromTeamConfirmationView, RemoveEgressFromTeamSuccessView}

import scala.concurrent.{ExecutionContext, Future}

class RemoveEgressFromTeamConfirmationController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  override val apiHubService: ApiHubService,
  override val errorResultBuilder: ErrorResultBuilder,
  hipEnvironments: HipEnvironments,
  confirmationView: RemoveEgressFromTeamConfirmationView,
  successView: RemoveEgressFromTeamSuccessView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Fetching {

  def onPageLoad(teamId: String, egressId: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      doWithTeamAndEgress(teamId, egressId)(
        (team, egress) =>
          Future.successful(Ok(confirmationView(RemoveEgressFromTeamConfirmationViewModel(team, egress, request.user))))
      )
  }

  def onSubmit(teamId: String, egressId: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      doWithTeamAndEgress(teamId, egressId)(
        (team, egress) =>
          apiHubService.removeEgressFromTeam(teamId, egressId).map {
            case Some(_) => Ok(successView(RemoveEgressFromTeamSuccessViewModel(team, request.user)))
            case None => errorResultBuilder.teamNotFound(teamId)
          }
      )
  }

  private def doWithTeamAndEgress(
    teamId: String,
    egressId: String
  )(f: (team: Team, egress: EgressGateway) => Future[Result])(implicit request: IdentifierRequest[?]): Future[Result] = {
    (for {
      team <- EitherT(fetchTeamOrNotFound(teamId))
      egress <- EitherT(fetchEgressOrNotFound(egressId, hipEnvironments))
    } yield f.apply(team, egress)).value.flatMap {
      case Right(result) => result
      case Left(result) => Future.successful(result)
    }
  }

}
