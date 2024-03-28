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

package controllers.team

import controllers.actions.IdentifierAction
import controllers.helpers.ErrorResultBuilder
import models.application.TeamMember
import models.requests.IdentifierRequest
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.CreateTeamStartPage
import play.api.i18n.I18nSupport
import play.api.mvc._
import repositories.CreateTeamSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CreateTeamStartController @Inject()(
    override val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    createTeamSessionRepository: CreateTeamSessionRepository,
    clock: Clock,
    navigator: Navigator,
    errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def startCreateTeam(): Action[AnyContent] = identify.async {
    implicit request =>
      request.user.email match {
        case Some(userEmail) => startJourney(buildUserAnswers(userEmail, request))
        case None => Future.successful(errorResultBuilder.internalServerError(s"No email found for user '${request.user.userId}'"))
      }
  }

  private def buildUserAnswers(userEmail: String, request: IdentifierRequest[_]): Try[UserAnswers] = {
    UserAnswers(
      id = request.user.userId,
      lastUpdated = clock.instant()
    ).set(CreateTeamStartPage, Seq(TeamMember(userEmail)))
  }

  private def startJourney(userAnswers: Try[UserAnswers]) = {
    for {
      userAnswers <- Future.fromTry(userAnswers)
      _           <- createTeamSessionRepository.set(userAnswers)
    } yield Redirect(navigator.nextPage(CreateTeamStartPage, NormalMode, userAnswers))
  }

}
