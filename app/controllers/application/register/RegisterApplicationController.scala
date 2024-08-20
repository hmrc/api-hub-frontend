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

package controllers.application.register

import com.google.inject.{Inject, Singleton}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.application.{Creator, NewApplication}
import models.user.UserModel
import models.{CheckMode, UserAnswers}
import pages.application.register.{RegisterApplicationNamePage, RegisterApplicationTeamPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.register.RegisterApplicationSuccessView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterApplicationController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  apiHubService: ApiHubService,
  sessionRepository: SessionRepository,
  view: RegisterApplicationSuccessView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def register(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateAndBuildApplication(request.userAnswers, request.user).fold(
        call => Future.successful(Redirect(call)),
        newApplication =>
          for {
            application <- apiHubService.registerApplication(newApplication)
            _ <- sessionRepository.clear(request.user.userId)
          } yield Ok(view(application, request.user))
      )
  }

  private def validateAndBuildApplication(userAnswers: UserAnswers, user: UserModel): Either[Call, NewApplication] = {
    for {
      applicationName <- validateApplicationName(userAnswers)
      teamId <- validateTeam(userAnswers)
    } yield NewApplication(applicationName, Creator(email = user.email.getOrElse("")), teamId)
  }

  private def validateApplicationName(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(RegisterApplicationNamePage) match {
      case Some(applicationName) => Right(applicationName)
      case _ => Left(controllers.application.register.routes.RegisterApplicationNameController.onPageLoad(CheckMode))
    }
  }

  private def validateTeam(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(RegisterApplicationTeamPage) match {
      case Some(team) => Right(team.id)
      case _ => Left(controllers.application.register.routes.RegisterApplicationTeamController.onPageLoad(CheckMode))
    }
  }

}
