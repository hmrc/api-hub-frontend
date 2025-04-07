/*
 * Copyright 2025 HM Revenue & Customs
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

import config.HipEnvironments
import controllers.actions.*
import controllers.routes
import forms.myapis.produce.ProduceApiSelectEgressForm
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.myapis.produce.{ProduceApiChooseTeamPage, ProduceApiSelectEgressPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.{ProduceApiSelectEgressFormViewModel, ProduceApiSelectEgressViewModel}
import views.html.myapis.produce.ProduceApiSelectEgressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiSelectEgressController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: ProduceApiSessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: ProduceApiDataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       apiHubService: ApiHubService,
                                       formProvider: ProduceApiSelectEgressForm,
                                       val controllerComponents: MessagesControllerComponents,
                                       hipEnvironments: HipEnvironments,
                                       view: ProduceApiSelectEgressView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(ProduceApiSelectEgressPage) match {
        case None => form
        case Some(egress) => form.fill(ProduceApiSelectEgressFormViewModel(egress, false))
      }

      buildView(Ok, preparedForm, mode)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => buildView(BadRequest, formWithErrors, mode),
        formValues =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiSelectEgressPage, formValues.value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiSelectEgressPage, mode, updatedAnswers))
      )

  }

  private def buildView(status: Status, form: Form[?], mode: Mode)(implicit request: DataRequest[AnyContent])= {
    request.userAnswers.get(ProduceApiChooseTeamPage) match {
      case Some(team) =>
        apiHubService.listEgressGateways(hipEnvironments.deployTo).map(egressGateways => {
          val teamEgresses = egressGateways.filter(e => team.egresses.contains(e.id))
          val viewModel = ProduceApiSelectEgressViewModel("myApis.produce.selectegress.title", teamEgresses, controllers.myapis.produce.routes.ProduceApiSelectEgressController.onSubmit(mode))
          status(view(form, viewModel, request.user))
        })
      case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
