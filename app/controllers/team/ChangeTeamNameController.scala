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

import controllers.actions._
import controllers.helpers.ErrorResultBuilder
import forms.ChangeTeamNameFormProvider
import models.exception.TeamNameNotUniqueException
import models.requests.TeamRequest
import models.user.UserModel
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.{ChangeTeamNameView, TeamUpdatedSuccessfullyView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeTeamNameController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        teamAuth: TeamAuthActionProvider,
                                        formProvider: ChangeTeamNameFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ChangeTeamNameView,
                                        successView: TeamUpdatedSuccessfullyView,
                                        apiHubService: ApiHubService,
                                        errorResultBuilder: ErrorResultBuilder
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen teamAuth(id)) async {
    implicit request =>
      getTeamName(id).flatMap(name => {
        Future.successful(Ok(view(form.fill(name), controllers.team.routes.ChangeTeamNameController.onSubmit(id), request.identifierRequest.user)))
      })
  }

  private def getTeamName(id: String)(implicit request: TeamRequest[_]) = {
    apiHubService.findTeamById(id).map {
        case Some(team) => team.name
        case None => Messages("apiDetails.details.team.error")
      }
    }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen teamAuth(id)) async  {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => badRequest(id, formWithErrors),
        name => changeTeamName(id, name, request.identifierRequest.user, form))
  }

  private def badRequest(id: String, formWithErrors: Form[String])(implicit request: TeamRequest[_]): Future[Result] = {
    Future.successful(BadRequest(view(formWithErrors, controllers.team.routes.ChangeTeamNameController.onSubmit(id), request.identifierRequest.user)))
  }

  private def changeTeamName(id: String, name: String, user: UserModel, form: Form[String])(implicit request: Request[_]) = {
    apiHubService.changeTeamName(id, name).map {
      case Right(_) => Ok(successView(user))
      case Left(_: TeamNameNotUniqueException) => nameNotUnique(form.fill(name), id, user)
      case _ => teamNotFound(id)
    }
  }

  private def teamNotFound(id: String)(implicit request: Request[_]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.teamNotFoundHeading"),
      message = Messages("site.teamNotFoundMessage", id)
    )
  }

  private def nameNotUnique(form: Form[String], id: String, user: UserModel)(implicit request: Request[_]) = {
    BadRequest(view(form.withError(FormError("value", "createTeamName.error.nameNotUnique")), controllers.team.routes.ChangeTeamNameController.onSubmit(id), user))
  }


}
