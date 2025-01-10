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

import config.FrontendAppConfig
import controllers.actions.*
import models.Mode
import pages.myapis.produce.{ProduceApiEgressAvailabilityPage, ProduceApiEgressSelectionPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEgressAvailabilityView
import forms.myapis.produce.ProduceApiEgressAvailabilityFormProvider
import navigation.Navigator
import viewmodels.myapis.produce.ProduceApiEgressAvailabilityViewModel

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiEgressAvailabilityController @Inject()(
                                                        override val messagesApi: MessagesApi,
                                                        sessionRepository: ProduceApiSessionRepository,
                                                        identify: IdentifierAction,
                                                        getData: ProduceApiDataRetrievalAction,
                                                        requireData: DataRequiredAction,
                                                        navigator: Navigator,
                                                        val controllerComponents: MessagesControllerComponents,
                                                        formProvider: ProduceApiEgressAvailabilityFormProvider,
                                                        view: ProduceApiEgressAvailabilityView,
                                                        config: FrontendAppConfig
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  private def viewModel(mode: Mode) = ProduceApiEgressAvailabilityViewModel(
    config.helpDocsPath,
    routes.ProduceApiEgressAvailabilityController.onSubmit(mode)
  )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiEgressAvailabilityPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, viewModel(mode), request.user))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(mode), request.user))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiEgressAvailabilityPage, value))
            updatedAnswers <- if (!value) {
              Future.fromTry(updatedAnswers.remove(ProduceApiEgressSelectionPage))
            } else {
              Future.successful(updatedAnswers)
            }
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiEgressAvailabilityPage, mode, updatedAnswers))
      )

  }

}
