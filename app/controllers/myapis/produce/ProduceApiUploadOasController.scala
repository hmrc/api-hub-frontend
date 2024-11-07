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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MultipartFormData, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiUploadOasView
import forms.myapis.produce.ProduceApiUploadOasFormProvider
import pages.myapis.produce.ProduceApiUploadOasPage
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import config.FrontendAppConfig
import play.api.libs.Files.TemporaryFile

class ProduceApiUploadOasController @Inject()(
                                        override val messagesApi: MessagesApi,
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

      Ok(view(preparedForm, mode, request.user, frontendAppConfig.maxOasUploadSizeMb))
  }

  def onSubmit(mode: Mode): Action[MultipartFormData[TemporaryFile]] = (identify andThen getData andThen requireData).async(parse.multipartFormData) {
    implicit request: Request[MultipartFormData[TemporaryFile]]  =>  {
      request.body.file("oasFile") match {
        case Some(file) => {
          val data = file.transformRefToBytes().utf8String
          Future.successful(Redirect(routes.ProduceApiEnterOasController.onPageLoad(mode)))
        }
        case None =>
          Future.successful(Redirect(routes.ProduceApiUploadOasController.onPageLoad(mode)))
      }

    }
  }
}
