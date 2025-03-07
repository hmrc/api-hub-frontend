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

import controllers.actions.*
import forms.myapis.produce.ProduceApiEnterWiremockFormProvider
import models.Mode
import navigation.Navigator
import pages.myapis.update.{UpdateApiEnterWiremockPage, UpdateApiUploadWiremockPage}
import play.api.i18n.{I18nSupport, MessagesApi, MessagesProvider}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UpdateApiSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEnterWiremockView
import viewmodels.myapis.produce.ProduceApiEnterWiremockViewModel

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiEnterWiremockController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  sessionRepository: UpdateApiSessionRepository,
                                                  navigator: Navigator,
                                                  identify: IdentifierAction,
                                                  getData: UpdateApiDataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  formProvider: ProduceApiEnterWiremockFormProvider,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: ProduceApiEnterWiremockView,
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  private def viewModel(mode: Mode) = ProduceApiEnterWiremockViewModel(
    formAction = routes.UpdateApiEnterWiremockController.onSubmit(mode), false
  )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(UpdateApiEnterWiremockPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, request.user, viewModel(mode)))
  }

  def onPageLoadWithUploadedWiremock(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(UpdateApiEnterWiremockPage).orElse(request.userAnswers.get(UpdateApiUploadWiremockPage).map(_.fileContents)) match {
        case Some(wiremockFileContents) => form.fill(wiremockFileContents)
        case None => form
      }
      Ok(view(preparedForm, request.user, viewModel(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val boundedForm = form.bindFromRequest()

      boundedForm.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, request.user, viewModel(mode)))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateApiEnterWiremockPage, value)) 
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiEnterWiremockPage, mode, updatedAnswers))
      )
  }

}
