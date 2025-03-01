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
import models.Mode
import models.requests.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MultipartFormData, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiUploadOasView
import forms.myapis.produce.ProduceApiUploadOasFormProvider
import pages.myapis.produce.{ProduceApiEnterOasPage, ProduceApiUploadOasPage}
import play.api.data.Form
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import config.FrontendAppConfig
import models.myapis.produce.ProduceApiUploadedOasFile
import navigation.Navigator
import repositories.ProduceApiSessionRepository
import viewmodels.myapis.produce.ProduceApiUploadOasViewModel
import models.myapis.produce.ProduceApiUploadedOasFile

class ProduceApiUploadOasController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: ProduceApiSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        formProvider: ProduceApiUploadOasFormProvider,
                                        getData: ProduceApiDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        view: ProduceApiUploadOasView,
                                        frontendAppConfig: FrontendAppConfig
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(ProduceApiUploadOasPage) match {
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
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(ProduceApiEnterOasPage))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(ProduceApiUploadOasPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiUploadOasPage, mode, updatedAnswers))
      )
    }
  }

  private def buildView(form: Form[ProduceApiUploadedOasFile], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiUploadOasViewModel(routes.ProduceApiUploadOasController.onSubmit(mode)), request.user, frontendAppConfig)
  }

}
