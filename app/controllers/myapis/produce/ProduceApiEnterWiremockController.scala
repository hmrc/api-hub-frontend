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
import forms.myapis.produce.ProduceApiEnterWiremockFormProvider
import models.Mode
import navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi, MessagesProvider}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEnterWiremockView
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.myapis.produce.ProduceApiEnterWiremockViewModel
import pages.myapis.produce.{ProduceApiEnterWiremockPage, ProduceApiUploadWiremockPage}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

class ProduceApiEnterWiremockController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   sessionRepository: ProduceApiSessionRepository,
                                                   navigator: Navigator,
                                                   identify: IdentifierAction,
                                                   getData: ProduceApiDataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   formProvider: ProduceApiEnterWiremockFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: ProduceApiEnterWiremockView
                                                 )
                                                 (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  private def viewModel(mode: Mode) = ProduceApiEnterWiremockViewModel(
    formAction = routes.ProduceApiEnterWiremockController.onSubmit(mode), true
  )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiEnterWiremockPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, request.user, viewModel(mode)))
  }

  def onPageLoadWithUploadedWiremock(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(ProduceApiEnterWiremockPage).orElse(request.userAnswers.get(ProduceApiUploadWiremockPage).map(_.fileContents)) match {
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiEnterWiremockPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiEnterWiremockPage, mode, updatedAnswers))
      )
  }

}
