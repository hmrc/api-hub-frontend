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
import forms.application.UpdateApplicationTeamFormProvider
import models.requests.ApplicationRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.{UpdateApplicationTeamSuccessView, UpdateApplicationTeamView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateApplicationTeamController @Inject()(
                                                 override val controllerComponents: MessagesControllerComponents,
                                                 identify: IdentifierAction,
                                                 applicationAuth: ApplicationAuthActionProvider,
                                                 formProvider: UpdateApplicationTeamFormProvider,
                                                 apiHubService: ApiHubService,
                                                 view: UpdateApplicationTeamView,
                                                 successView: UpdateApplicationTeamSuccessView,
                                                 errorResultBuilder: ErrorResultBuilder
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(applicationId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId, includeDeleted = true)) async {
    implicit request => showView(OK, form)
  }

  private def showView(code: Int, form: Form[?])(implicit request: ApplicationRequest[?]): Future[Result] = {
    apiHubService.findTeams(None).map(teams => {
      val sortedTeams = teams.sortBy(_.name.toLowerCase)
      val owningTeam = teams.find(team => request.application.teamId.contains(team.id))
      Status(code)(view(form, request.application, owningTeam, sortedTeams, request.identifierRequest.user))
    })
  }

  def onSubmit(applicationId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId, includeDeleted = true)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          showView(BAD_REQUEST, formWithErrors)
        },
        maybeTeamId =>
          apiHubService.updateApplicationTeam(applicationId, maybeTeamId) map   {
            case Some(()) => Ok(successView(request.application, request.identifierRequest.user))
            case None => somethingNotFound()
          }
      )
  }

  private def somethingNotFound()(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("application.update.team.something.not.found.heading"),
      message = Messages("application.update.team.something.not.found.message")
    )
  }
}
