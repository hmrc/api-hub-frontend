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
import controllers.actions.IdentifierAction
import controllers.helpers.ErrorResultBuilder
import models.application.TeamMember
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.application.register.{RegisterApplicationStartPage, RegisterApplicationTeamMembersPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterApplicationStartController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  sessionRepository: SessionRepository,
  errorResultBuilder: ErrorResultBuilder,
  navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController {

  def start(): Action[AnyContent] = identify.async {
    implicit request =>
      request.user.email.fold(noEmail())(
        email =>
          for {
            userAnswers <- Future.fromTry(UserAnswers(request.user.userId).set(RegisterApplicationTeamMembersPage, Seq(TeamMember(email))))
            _ <- sessionRepository.set(userAnswers)
          } yield Redirect(navigator.nextPage(RegisterApplicationStartPage, NormalMode, userAnswers))
      )
  }

  private def noEmail()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.internalServerError("The current user does not have an email address")
    )
  }

}
