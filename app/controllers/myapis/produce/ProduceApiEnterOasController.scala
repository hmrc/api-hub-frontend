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
import forms.myapis.produce.ProduceApiEnterOasFormProvider
import models.Mode
import models.curl.OpenApiDoc
import navigation.Navigator
import pages.myapis.produce.ProduceApiEnterOasPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi, MessagesProvider}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEnterOasView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiEnterOasController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: ProduceApiSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: ProduceApiDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: ProduceApiEnterOasFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ProduceApiEnterOasView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiEnterOasPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val boundedForm = form.bindFromRequest()

      boundedForm.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          validateOAS(value).fold(
            error =>
              Future.successful(BadRequest(view(boundedForm.withGlobalError(error), mode))),
            _ => for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiEnterOasPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ProduceApiEnterOasPage, mode, updatedAnswers))
          )
      )
  }

  private def validateOAS(oas: String)(implicit messagesProvider: MessagesProvider): Either[String, Unit] =
    OpenApiDoc.parse(oas).map(_ => ())
}
