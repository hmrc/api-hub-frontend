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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.myapis.update.routes
import forms.myapis.produce.ProduceApiAddPrefixesFormProvider
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.myapis.update.{UpdateApiAddPrefixesPage, UpdateApiEgressPrefixesPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UpdateApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.ProduceApiAddPrefixesViewModel
import views.html.myapis.produce.ProduceApiAddPrefixesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiAddPrefixesController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                config: FrontendAppConfig,
                                                sessionRepository: UpdateApiSessionRepository,
                                                navigator: Navigator,
                                                identify: IdentifierAction,
                                                getData: UpdateApiDataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                val controllerComponents: MessagesControllerComponents,
                                                formProvider: ProduceApiAddPrefixesFormProvider,
                                                view: ProduceApiAddPrefixesView
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()
  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(UpdateApiAddPrefixesPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(buildView(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(buildView(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateApiAddPrefixesPage, value))
            updatedAnswers <- if (!value) {
              Future.fromTry(updatedAnswers.remove(UpdateApiEgressPrefixesPage))
            } else {
              Future.successful(updatedAnswers)
            }
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiAddPrefixesPage, mode, updatedAnswers))
      )
  }

  private def buildView(form: Form[Boolean], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiAddPrefixesViewModel(routes.UpdateApiAddPrefixesController.onSubmit(mode)), config.helpDocsPath, request.user)
  }

}
