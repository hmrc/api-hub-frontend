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

import com.google.inject.{Inject, Singleton}
import config.CryptoProvider
import controllers.actions.{IdentifierAction, TeamAuthActionProvider}
import controllers.helpers.ErrorResultBuilder
import models.application.Application
import models.team.TeamLenses._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.ManageTeamView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageTeamController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  teamAuth: TeamAuthActionProvider,
  view: ManageTeamView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  cryptoProvider: CryptoProvider
)(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private lazy val crypto = cryptoProvider.get()

  def onPageLoad(id: String, applicationId: Option[String]): Action[AnyContent] = (identify andThen teamAuth(id)).async {
    implicit request =>
      fetchApplication(applicationId).map {
        case Right(application) =>
            Ok(view(request.team.withSortedTeam(), application, request.identifierRequest.user, crypto))
        case Left(result) => result
      }
  }

  private def fetchApplication(applicationId: Option[String])(implicit request: Request[?]): Future[Either[Result, Option[Application]]] = {
    applicationId match {
      case Some(id) => apiHubService.getApplication(id, false).map{
        case Some(application) => Right(Some(application))
        case None => Left(errorResultBuilder.applicationNotFound(id))
      }
      case None => Future.successful(Right(None))
    }
  }

}
