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

package controllers.application

import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.AddTeamMemberDetailsFormProvider
import models.application.ApplicationLenses._
import models.application.TeamMember
import models.requests.ApplicationRequest
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddTeamMemberDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddTeamMemberController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  formProvider: AddTeamMemberDetailsFormProvider,
  view: AddTeamMemberDetailsView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = false)).async {
    implicit request =>
      Future.successful(Ok(view(form, routes.AddTeamMemberController.onSubmit(id), request.identifierRequest.user)))
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = false)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          badRequest(id, formWithErrors),
        teamMember =>
          if (request.application.hasTeamMember(teamMember)) {
            val formWithErrors = form.fill(teamMember).withError(FormError("email", "addTeamMemberDetails.email.duplicate"))
            badRequest(id, formWithErrors)
          }
          else {
            addTeamMember(id, teamMember)
          }
      )
  }

  private def addTeamMember(id: String, teamMember: TeamMember)(implicit request: Request[?]) = {
    apiHubService.addTeamMember(id, teamMember).map {
      case Some(_) => Redirect(routes.ManageTeamMembersController.onPageLoad(id))
      case _ => applicationNotFound(id)
    }
  }

  private def badRequest(id: String, formWithErrors: Form[TeamMember])(implicit request: ApplicationRequest[?]) = {
    Future.successful(BadRequest(view(formWithErrors, routes.AddTeamMemberController.onSubmit(id), request.identifierRequest.user)))
  }

  private def applicationNotFound(id: String)(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.applicationNotFoundHeading"),
      message = Messages("site.applicationNotFoundMessage", id)
    )
  }

}
