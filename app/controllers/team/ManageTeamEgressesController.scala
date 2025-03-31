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
import models.requests.BaseRequest
import models.team.TeamLenses.*
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.ManageTeamEgressesView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageTeamEgressesController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  teamAuth: TeamAuthActionProvider,
  view: ManageTeamEgressesView
)(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen teamAuth(id)) {
    implicit request => Ok(view(request.team, request.identifierRequest.user))
  }

}
