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

import config.Hods
import controllers.actions.*
import forms.myapis.produce.ProduceApiHodFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.myapis.produce.ProduceApiHodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.ProduceApiHodViewModel
import views.html.myapis.produce.ProduceApiHodView

import scala.concurrent.{ExecutionContext, Future}

class ProduceApiHodController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: ProduceApiSessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: ProduceApiDataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: ProduceApiHodFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: ProduceApiHodView,
                                         hods: Hods,
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  private def viewModel(mode: Mode) =
    ProduceApiHodViewModel(
      "produceApiHod.title",
      controllers.myapis.produce.routes.ProduceApiHodController.onSubmit(mode)
    )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiHodPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, request.user, hods, viewModel(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, request.user, hods, viewModel(mode)))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiHodPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiHodPage, mode, updatedAnswers))
      )
  }
}
