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

package controllers.team

import controllers.actions._
import models.application.TeamMember
import models.exception.TeamNameNotUniqueException
import models.{CheckMode, UserAnswers}
import models.team.NewTeam
import pages.{CreateTeamMembersPage, CreateTeamNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.CreateTeamSuccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateTeamController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: CreateTeamDataRetrievalAction,
  requireData: DataRequiredAction,
  apiHubService: ApiHubService,
  sessionRepository: CreateTeamSessionRepository,
  view: CreateTeamSuccessView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        newTeam =>
          apiHubService.createTeam(newTeam).flatMap {
            case Right(team) =>
              for {
                _ <- sessionRepository.clear(request.userAnswers.id)
              } yield Ok(view(team, Some(request.user)))
            case Left(_: TeamNameNotUniqueException) =>
              Future.successful(Redirect(controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode)))
            case Left(e) => throw e
          }
      )
  }

  private def validate(userAnswers: UserAnswers): Either[Call, NewTeam] = {
    for {
      name <- validateTeamName(userAnswers)
      teamMembers <- validateTeamMembers(userAnswers)
    } yield NewTeam(name, teamMembers)
  }

  private def validateTeamName(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(CreateTeamNamePage) match {
      case Some(name) => Right(name)
      case _ => Left(controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode))
    }
  }

  private def validateTeamMembers(userAnswers: UserAnswers): Either[Call, Seq[TeamMember]] = {
    userAnswers.get(CreateTeamMembersPage) match {
      case Some(teamMembers) => Right(teamMembers)
      case _ => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

}
