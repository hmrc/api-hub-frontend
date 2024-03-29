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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{CheckMode, UserAnswers}
import models.application.{Creator, NewApplication, TeamMember}
import models.user.UserModel
import pages.{ApplicationNamePage, TeamMembersPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class RegisterApplicationController @Inject()(
    override val messagesApi: MessagesApi,
    override val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    apiHubService: ApiHubService,
    sessionRepository: SessionRepository
  )(implicit ec: ExecutionContext)
  extends FrontendBaseController
  with I18nSupport {

  def create(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateAndBuildApplication(request.userAnswers, request.user).fold(
        call => Future.successful(Redirect(call)),
        newApplication =>
          for {
            application <- apiHubService.registerApplication(newApplication)
            _ <- sessionRepository.clear(request.user.userId)
          } yield Redirect(routes.RegisterApplicationSuccessController.onPageLoad(application.id))
      )
  }

  private def validateAndBuildApplication(userAnswers: UserAnswers, user: UserModel): Either[Call, NewApplication] = {
    for {
      applicationName <- validateApplicationName(userAnswers)
      teamMembers <- validateTeamMembers(userAnswers)
    } yield NewApplication(applicationName, Creator(email = user.email.getOrElse("")), teamMembers)
  }

  private def validateApplicationName(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ApplicationNamePage) match {
      case Some(applicationName) => Right(applicationName)
      case _ => Left(routes.ApplicationNameController.onPageLoad(CheckMode))
    }
  }

  private def validateTeamMembers(userAnswers: UserAnswers): Either[Call, Seq[TeamMember]] = {
    userAnswers.get(TeamMembersPage) match {
      case Some(teamMembers) if teamMembers.nonEmpty => Right(teamMembers)
      case _ => Left(routes.QuestionAddTeamMembersController.onPageLoad(CheckMode))
    }
  }

}
