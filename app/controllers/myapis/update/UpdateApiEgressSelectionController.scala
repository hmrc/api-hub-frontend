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

package controllers.myapis.update

import config.{FrontendAppConfig, HipEnvironments}
import controllers.actions.*
import forms.myapis.produce.ProduceApiEgressSelectionForm
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.myapis.update.{UpdateApiEgressAvailabilityPage, UpdateApiEgressSelectionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UpdateApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEgressSelectionView
import viewmodels.myapis.produce.ProduceApiEgressSelectionViewModel

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiEgressSelectionController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    identify: IdentifierAction,
                                                    getData: UpdateApiDataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: ProduceApiEgressSelectionView,
                                                    config: FrontendAppConfig,
                                                    formProvider: ProduceApiEgressSelectionForm,
                                                    sessionRepository: UpdateApiSessionRepository,
                                                    apiHubService: ApiHubService,
                                                    navigator: Navigator,
                                                    hipEnvironments: HipEnvironments
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(UpdateApiEgressSelectionPage) match {
        case None => form
        case Some(egressChoices) => form.fill(egressChoices)
      }

      buildView(mode, preparedForm, Ok)

  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      form.bindFromRequest().fold(
        formWithErrors => buildView(mode, formWithErrors, BadRequest),

        egressChoices =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateApiEgressSelectionPage, egressChoices))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiEgressSelectionPage, mode, updatedAnswers))
      )
    }
  }

  private def buildView(mode: Mode, form: Form[String], status: Status)(implicit request: DataRequest[AnyContent]) = {
    val viewModel = ProduceApiEgressSelectionViewModel(
      "myApis.update.selectegress.title",
      controllers.myapis.update.routes.UpdateApiEgressSelectionController.onSubmit(mode),
      controllers.myapis.update.routes.UpdateApiEgressAvailabilityController.onPageLoad(mode).url,
      false
    )
    apiHubService.listEgressGateways(hipEnvironments.deploymentHipEnvironment).map(
      egressGateways =>
        status(view(form, request.user, config.helpDocsPath, egressGateways, viewModel))
    )
  }
}
