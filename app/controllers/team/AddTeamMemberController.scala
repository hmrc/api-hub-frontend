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

import com.google.inject.Inject
import controllers.actions.{IdentifierAction, TeamAuthActionProvider}
import controllers.helpers.ErrorResultBuilder
import forms.AddTeamMemberDetailsFormProvider
import models.application.TeamMember
import models.requests.TeamRequest
import models.team.TeamLenses._
import models.user.UserModel
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddTeamMemberDetailsView
import views.html.team.TeamUpdatedSuccessfullyView

import scala.concurrent.{ExecutionContext, Future}

class AddTeamMemberController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  teamAuth: TeamAuthActionProvider,
  formProvider: AddTeamMemberDetailsFormProvider,
  view: AddTeamMemberDetailsView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  successView: TeamUpdatedSuccessfullyView
)(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen teamAuth(id)) {
    implicit request =>
      Ok(view(form, controllers.team.routes.AddTeamMemberController.onSubmit(id), request.identifierRequest.user))
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen teamAuth(id)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => badRequest(id, formWithErrors),
        teamMember =>
          if (request.team.hasTeamMember(teamMember)) {
            val formWithErrors = form.fill(teamMember).withError(FormError("email", "addTeamMemberDetails.email.duplicate"))
            badRequest(id, formWithErrors)
          }
          else {
            addTeamMember(id, teamMember, request.identifierRequest.user)
          }
      )
  }

  private def badRequest(id: String, formWithErrors: Form[TeamMember])(implicit request: TeamRequest[_]): Future[Result] = {
    Future.successful(BadRequest(view(formWithErrors, controllers.team.routes.AddTeamMemberController.onSubmit(id), request.identifierRequest.user)))
  }

  private def addTeamMember(id: String, teamMember: TeamMember, user: UserModel)(implicit request: Request[_]) = {
    apiHubService.addTeamMemberToTeam(id, teamMember).map {
      case Some(_) => Ok(successView(user))
      case _ => teamNotFound(id)
    }
  }

  private def teamNotFound(id: String)(implicit request: Request[_]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.teamNotFoundHeading"),
      message = Messages("site.teamNotFoundMessage", id)
    )
  }

}
