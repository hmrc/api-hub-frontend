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
import forms.myapis.produce.ProduceApiUploadOasFormProvider
import models.Mode
import navigation.Navigator
import models.requests.DataRequest
import pages.myapis.update.{UpdateApiEnterOasPage, UpdateApiUploadOasPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.UpdateApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.ProduceApiUploadOasViewModel
import views.html.myapis.produce.ProduceApiUploadOasView
import play.api.data.Form
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.myapis.produce.ProduceApiUploadedOasFile

class UpdateApiUploadOasController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: UpdateApiSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        formProvider: ProduceApiUploadOasFormProvider,
                                        getData: UpdateApiDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        view: ProduceApiUploadOasView,
                                        frontendAppConfig: FrontendAppConfig
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(UpdateApiUploadOasPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(buildView(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(buildView(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(UpdateApiEnterOasPage))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(UpdateApiUploadOasPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiUploadOasPage, mode, updatedAnswers))
      )
    }
  }

  private def buildView(form: Form[ProduceApiUploadedOasFile], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiUploadOasViewModel(routes.UpdateApiUploadOasController.onSubmit(mode)), request.user, frontendAppConfig)
  }
  
}
