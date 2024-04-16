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

import controllers.actions._
import controllers.routes
import models.NormalMode
import navigation.Navigator
import pages.CreateTeamMembersPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.team.ManageTeamMembers
import views.html.team.ManageTeamMembersView

import javax.inject.Inject

class ManageTeamMembersController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                navigator: Navigator,
                                                identify: IdentifierAction,
                                                getData: CreateTeamDataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: ManageTeamMembersView,
                                              ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = { (identify andThen getData andThen requireData) {
    implicit request =>
      val maybeSummaryListRows = for {
        currentUserEmail <- request.user.email
        teamMemberList <- request.userAnswers.get(CreateTeamMembersPage)
      } yield SummaryList(rows=ManageTeamMembers.rows(currentUserEmail, teamMemberList))

      maybeSummaryListRows match {
        case Some(summaryListRows) => Ok(view(summaryListRows, request.user))
        case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }}

  def onContinue: Action[AnyContent] = { (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(CreateTeamMembersPage, NormalMode, request.userAnswers))
  }}

}
