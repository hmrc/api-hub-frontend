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

package controllers.myapis

import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.myapis.UpdateApiTeamFormProvider
import models.requests.ApiRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.{UpdateApiTeamSuccessView, UpdateApiTeamView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateApiTeamController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  apiAuth: ApiAuthActionProvider,
  formProvider: UpdateApiTeamFormProvider,
  apiHubService: ApiHubService,
  view: UpdateApiTeamView,
  successView: UpdateApiTeamSuccessView,
  errorResultBuilder: ErrorResultBuilder
  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(apiId: String): Action[AnyContent] = (identify andThen apiAuth(apiId)) async {
    implicit request => showView(OK, form)
  }

  private def showView(code: Int, form: Form[?])(implicit request: ApiRequest[?]): Future[Result] = {
    apiHubService.findTeams(None).map(teams => {
      val owningTeam = teams.find(team => request.apiDetails.teamId.contains(team.id))
      val sortedTeams = teams.sortBy(_.name.toLowerCase)
      Status(code)(view(form, request.apiDetails, owningTeam, sortedTeams, request.identifierRequest.user))
    })
  }

  def onSubmit(apiId: String): Action[AnyContent] = (identify andThen apiAuth(apiId)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          showView(BAD_REQUEST, formWithErrors)
        },
        teamId =>
          apiHubService.updateApiTeam(apiId, teamId) map   {
            case Some(()) => Ok(successView(request.apiDetails, request.identifierRequest.user))
            case None => somethingNotFound()
          }
      )
  }

  private def somethingNotFound()(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("myApis.update.team.something.not.found.heading"),
      message = Messages("myApis.update.team.something.not.found.message")
    )
  }
}
