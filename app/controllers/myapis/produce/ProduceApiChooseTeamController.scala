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

package controllers.myapis.produce

import controllers.actions.*
import controllers.helpers.ErrorResultBuilder
import forms.myapis.produce.ProduceApiChooseTeamFormProvider
import models.Mode
import models.requests.{BaseRequest, DataRequest}
import navigation.Navigator
import pages.myapis.produce.ProduceApiChooseTeamPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiChooseTeamView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiChooseTeamController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        produceApiSessionRepository: ProduceApiSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: ProduceApiDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: ProduceApiChooseTeamFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ProduceApiChooseTeamView,
                                        apiHubService: ApiHubService,
                                        errorResultBuilder: ErrorResultBuilder
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiChooseTeamPage) match {
        case None => form
        case Some(team) => form.fill(team.id)
      }

      buildView(mode, preparedForm, Ok)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          buildView(mode, formWithErrors, BadRequest),

        teamId =>
          apiHubService.findTeamById(teamId).flatMap {
            case Some(team) =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiChooseTeamPage, team))
                _ <- produceApiSessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(ProduceApiChooseTeamPage, mode, updatedAnswers))
            case None => teamNotFound(teamId)
          }
      )
  }

  private def buildView(mode: Mode, form: Form[String], status: Status)(implicit request: DataRequest[AnyContent]) = {
    apiHubService.findTeams(Some(request.user.email)).map(
      teams =>
        status(view(form, mode, teams.sortBy(_.name.toLowerCase()), request.user))
    )
  }
  private def teamNotFound(teamId: String)(implicit request: BaseRequest[?]): Future[Result] = {
    Future.successful(errorResultBuilder.teamNotFound(teamId))
  }
}
