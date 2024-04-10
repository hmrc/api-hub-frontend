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
import controllers.helpers.ErrorResultBuilder
import models.NormalMode
import navigation.Navigator
import pages.CreateTeamMembersPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.ManageTeamMembersView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ManageTeamMembersController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                navigator: Navigator,
                                                identify: IdentifierAction,
                                                getData: CreateTeamDataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: ManageTeamMembersView,
                                                errorResultBuilder: ErrorResultBuilder
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad: Action[AnyContent] = { (identify andThen getData andThen requireData) {
    implicit request =>
      val maybeTeamMemberList = for {
        userEmail <- request.user.email
        allTeamMemberDetails <- request.userAnswers.get(CreateTeamMembersPage)
        (currentUser, otherTeamMembers) = allTeamMemberDetails.partition(teamMember => emailAddressesMatch(teamMember.email, userEmail))
        teamMemberList = currentUser ++ otherTeamMembers.sortBy(_.email)
      } yield teamMemberList

      maybeTeamMemberList match {
        case Some(teamMemberList) => Ok(view(teamMemberList, request.user))
        case None => errorResultBuilder.internalServerError("Unable to construct the team member list")
      }
  }}

  def onContinue: Action[AnyContent] = { (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(CreateTeamMembersPage, NormalMode, request.userAnswers))
  }}

  private def emailAddressesMatch(email1: String, email2: String): Boolean = formatEmailAddress(email1) == formatEmailAddress(email2)

  private def formatEmailAddress(email: String): String = email.toLowerCase.trim
}
