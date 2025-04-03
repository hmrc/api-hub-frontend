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
import controllers.actions.{AddEgressToTeamDataRetrievalAction, AuthorisedSupportAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.team.Team
import pages.admin.addegresstoteam.AddEgressToTeamTeamPage
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.admin.addegresstoteam.TeamEgressSuccessView

import scala.concurrent.ExecutionContext

@Singleton
class TeamEgressSuccessController @Inject()(
                                             override val controllerComponents: MessagesControllerComponents,
                                             identify: IdentifierAction,
                                             isSupport: AuthorisedSupportAction,
                                             getData: AddEgressToTeamDataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             errorResultBuilder: ErrorResultBuilder,
                                             view: TeamEgressSuccessView
                                           )(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData) {
    implicit request => 
      request.userAnswers.get(AddEgressToTeamTeamPage) match {
        case None => errorResultBuilder.teamNotFound("unknown")
        case Some(team) => Ok(view(team, request.user))
      }
    }
}
