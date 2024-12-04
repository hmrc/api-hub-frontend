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
import forms.myapis.produce.ProduceApiUploadWiremockFormProvider
import models.Mode
import models.myapis.produce.{ProduceApiUploadedOasFile, ProduceApiUploadedWiremockFile}
import models.requests.DataRequest
import navigation.Navigator
import org.apache.pekko.actor.Status.Success
import org.bson.json.JsonObject
import pages.myapis.produce.{ProduceApiEnterWiremockPage, ProduceApiUploadWiremockPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.*
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.{ProduceApiUploadOasViewModel, ProduceApiUploadWiremockViewModel}
import views.html.myapis.produce.{ProduceApiUploadOasView, ProduceApiUploadWiremockView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiUploadWiremockController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               sessionRepository: ProduceApiSessionRepository,
                                               navigator: Navigator,
                                               identify: IdentifierAction,
                                               val controllerComponents: MessagesControllerComponents,
                                               formProvider: ProduceApiUploadWiremockFormProvider,
                                               getData: ProduceApiDataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               view: ProduceApiUploadWiremockView,
                                               frontendAppConfig: FrontendAppConfig
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(ProduceApiUploadWiremockPage) match {
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
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(ProduceApiEnterWiremockPage))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(ProduceApiUploadWiremockPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiUploadWiremockPage, mode, updatedAnswers))
      )
    }
  }

  private def isValidWiremock(value: String): Boolean = {
    scala.util.Try(play.api.libs.json.Json.parse(value)).toOption.isDefined
  }


  private def buildView(form: Form[ProduceApiUploadedWiremockFile], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiUploadWiremockViewModel(routes.ProduceApiUploadWiremockController.onSubmit(mode)), request.user, frontendAppConfig)
  }

}
