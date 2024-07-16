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
import config.FrontendAppConfig
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import forms.myapis.UpdateApiTeamFormProvider
import models.requests.ApiRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.UpdateApiTeamView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateApiTeamController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  apiAuth: ApiAuthActionProvider,
  formProvider: UpdateApiTeamFormProvider,
  apiHubService: ApiHubService,
  view: UpdateApiTeamView,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  private val form = formProvider()

  def onPageLoad(apiId: String): Action[AnyContent] = (identify andThen apiAuth(apiId)) async {
    implicit request => showView(OK, form)
  }

  private def showView(code: Int, form: Form[_])(implicit request: ApiRequest[_]): Future[Result] = {
    apiHubService.findTeams(None).map(teams => {
      val owningTeam = teams.find(team => request.apiDetails.teamId.contains(team.id))
      Status(code)(view(form, request.apiDetails, owningTeam, teams, request.identifierRequest.user, config.supportEmailAddress))
    })
  }

  def onSubmit(apiId: String): Action[AnyContent] = (identify andThen apiAuth(apiId)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => showView(BAD_REQUEST, formWithErrors),
        teamId =>
          apiHubService.updateApiTeam(apiId, teamId).map { _ =>
            Redirect(controllers.myapis.routes.UpdateApiTeamController.onPageLoad(apiId))
          }
      )
  }

}
