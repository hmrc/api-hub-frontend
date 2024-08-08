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

import config.CryptoProvider
import controllers.actions._
import controllers.helpers.ErrorResultBuilder
import pages.CreateTeamMembersPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.{RemoveTeamMemberConfirmationView, RemoveTeamMemberSuccessView}
import forms.YesNoFormProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveTeamMemberController @Inject()(override val messagesApi: MessagesApi,
                                           sessionRepository: CreateTeamSessionRepository,
                                           identify: IdentifierAction,
                                           getData: CreateTeamDataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           errorResultBuilder: ErrorResultBuilder,
                                           teamAuth: TeamAuthActionProvider,
                                           confirmationView: RemoveTeamMemberConfirmationView,
                                           successView: RemoveTeamMemberSuccessView,
                                           formProvider: YesNoFormProvider,
                                           apiHubService: ApiHubService,
                                           cryptoProvider: CryptoProvider,
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider("manageTeam.teamMembers.removeTeamMember.error")
  private lazy val crypto = cryptoProvider.get

  def removeTeamMember(index: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(CreateTeamMembersPage).fold[Future[Result]](teamMemberNotFound())(
        teamMembers => {
          if (index == 0 || teamMembers.length < index ) {
            teamMemberNotFound()
          } else {
            val members = teamMembers.patch(index, Nil, 1)
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CreateTeamMembersPage, members))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(routes.ManageTeamMembersController.onPageLoad().url)
          }
        }
      )
  }

  def removeTeamMemberFromExistingTeam(teamId: String, encryptedEmail: String): Action[AnyContent] = (identify andThen teamAuth(teamId)) {
    implicit request =>
      val email = decrypt(encryptedEmail)
      val team = request.team
      team.teamMembers.find(_.email.equalsIgnoreCase(email)).map(teamMember =>
        Ok(confirmationView(team, teamMember, encryptedEmail, request.identifierRequest.user, form))
      ).getOrElse(teamMemberToRemoveNotFound())
  }

  def onRemovalSubmit(teamId: String, encryptedEmail: String): Action[AnyContent] = (identify andThen teamAuth(teamId)).async {
    implicit request =>
      val email = decrypt(encryptedEmail)
      val team = request.team
      team.teamMembers.find(_.email.equalsIgnoreCase(email)).map(teamMember =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
                BadRequest(confirmationView(request.team, teamMember, encryptedEmail, request.identifierRequest.user, formWithErrors))
            ),
          value =>
            if (value) {
              val user = request.identifierRequest.user
              val isSelfRemoval = user.email.map(_.equalsIgnoreCase(email)).getOrElse(false)
              if (isSelfRemoval)
                teamMemberToRemoveSameAsUser()
              else
                apiHubService.removeTeamMemberFromTeam(teamId, teamMember).map {
                  case Some(_) => Ok(successView(team, user))
                  case None => teamMemberToRemoveNotFound()
                }
            }
            else {
              Future.successful(Redirect(controllers.team.routes.ManageTeamController.onPageLoad(teamId)))
            }
        )
      ).getOrElse(Future.successful(teamMemberToRemoveNotFound()))
  }

  private def decrypt(encrypted: String): String = {
    crypto.decrypt(Crypted(encrypted)).value
  }

  private def teamMemberNotFound()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.notFound(Messages("addTeamMemberDetails.notFound"))
    )
  }

  private def teamMemberToRemoveNotFound()(implicit request: Request[_]): Result =
    errorResultBuilder.notFound(Messages("manageTeam.teamMembers.removeTeamMember.notFound"))

  private def teamMemberToRemoveSameAsUser()(implicit request: Request[_]): Future[Result] =
    Future.successful(
      errorResultBuilder.badRequest(Messages("manageTeam.teamMembers.removeTeamMember.cantDeleteAuthenticatedUser"))
    )
}
