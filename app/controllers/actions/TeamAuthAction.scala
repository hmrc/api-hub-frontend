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

package controllers.actions

import com.google.inject.{Inject, Singleton}
import controllers.helpers.ErrorResultBuilder
import controllers.routes
import models.requests.{IdentifierRequest, TeamRequest}
import models.team.Team
import models.team.TeamLenses._
import models.user.UserModel
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait TeamAuthAction extends ActionRefiner[IdentifierRequest, TeamRequest]

trait TeamAuthActionProvider {

  def apply(teamId: String)(implicit ec: ExecutionContext): TeamAuthAction

}

@Singleton
class TeamAuthActionProviderImpl @Inject()(
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  override val messagesApi: MessagesApi
) extends TeamAuthActionProvider with I18nSupport {

  override def apply(teamId: String)(implicit ec: ExecutionContext): TeamAuthAction = {
    new TeamAuthAction with FrontendHeaderCarrierProvider {
      override protected def refine[A](identifierRequest: IdentifierRequest[A]): Future[Either[Result, TeamRequest[A]]] = {
        implicit val request: Request[?] = identifierRequest

        apiHubService.findTeamById(teamId) map {
          case Some(team) =>
            if (identifierRequest.user.permissions.canSupport || isTeamMember(team, identifierRequest.user)) {
              Right(TeamRequest(identifierRequest, team))
            }
            else {
              Left(Redirect(routes.UnauthorisedController.onPageLoad))
            }
          case None =>
            Left(
              errorResultBuilder.notFound(
                Messages("site.teamNotFoundHeading"),
                Messages("site.teamNotFoundMessage", teamId)
              )
            )
        }
      }

      override protected def executionContext: ExecutionContext = ec
    }
  }

  private def isTeamMember(team: Team, user: UserModel): Boolean = {
    team.hasTeamMember(user.email)
  }

}
