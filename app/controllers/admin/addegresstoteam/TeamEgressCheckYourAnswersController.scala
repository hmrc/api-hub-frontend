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

package controllers.admin.addegresstoteam

import com.google.inject.{Inject, Singleton}
import config.HipEnvironments
import controllers.actions.{AddEgressToTeamDataRetrievalAction, AuthorisedSupportAction, DataRequiredAction, IdentifierAction}
import controllers.routes
import models.UserAnswers
import models.team.Team
import pages.admin.addegresstoteam.{AddEgressToTeamTeamPage, SelectTeamEgressesPage}
import play.api.i18n.I18nSupport
import play.api.mvc.*
import repositories.AddEgressToTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.AssignTeamEgressesViewModel
import views.html.admin.addegresstoteam.{TeamEgressCheckYourAnswersView, TeamEgressSuccessView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TeamEgressCheckYourAnswersController @Inject()(
                                                      override val controllerComponents: MessagesControllerComponents,
                                                      identify: IdentifierAction,
                                                      isSupport: AuthorisedSupportAction,
                                                      cyaView: TeamEgressCheckYourAnswersView,
                                                      getData: AddEgressToTeamDataRetrievalAction,
                                                      requireData: DataRequiredAction,
                                                      apiHubService: ApiHubService,
                                                      hipEnvironments: HipEnvironments,
                                                      sessionRepository: AddEgressToTeamSessionRepository
                                                    )(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData) async {
    implicit request => {
        validate(request.userAnswers).fold(
          call => Future.successful(Redirect(call)),
          (team, egresses) => for {
            egressGateways <- apiHubService.listEgressGateways(hipEnvironments.deployTo)
          } yield Ok(cyaView(AssignTeamEgressesViewModel(team, egressGateways.filter(egressGateway => egresses.contains(egressGateway.id))), request.user)))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData).async {
    implicit request =>
      validate(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        (team, egresses) =>
          apiHubService.addEgressesToTeam(team.id, egresses).flatMap {
            case Some(()) =>
              for {
                _ <- sessionRepository.clear(request.userAnswers.id)
              } yield Redirect(controllers.admin.addegresstoteam.routes.TeamEgressSuccessController.onPageLoad())
            case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

  private def validate(userAnswers: UserAnswers): Either[Call, (Team, Set[String])] = {
    for {
      team <- validateTeam(userAnswers)
      egresses <- validateEgresses(userAnswers, team)
    } yield (team, egresses)
  }

  private def validateTeam(userAnswers: UserAnswers): Either[Call, Team] = {
    userAnswers.get(AddEgressToTeamTeamPage) match {
      case Some(team) => Right(team)
      case _ => Left(controllers.admin.routes.ManageTeamsController.onPageLoad())
    }
  }

  private def validateEgresses(userAnswers: UserAnswers, team: Team): Either[Call, Set[String]] = {
    userAnswers.get(SelectTeamEgressesPage) match {
      case Some(egresses) => Right(egresses)
      case _ => Left(controllers.team.routes.ManageTeamEgressesController.onPageLoad(team.id))
    }
  }

}
