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

import cats.data.EitherT
import connectors.ApplicationsConnector
import controllers.actions.*
import forms.myapis.produce.ProduceApiEnterOasFormProvider
import models.Mode
import models.curl.OpenApiDoc
import navigation.Navigator
import pages.myapis.produce.{ProduceApiEnterApiTitlePage, ProduceApiEnterOasPage}
import play.api.i18n.*
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
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
                                        view: ProduceApiEnterOasView,
                                        applicationsConnector: ApplicationsConnector
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiEnterOasPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, request.user))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val boundedForm = form.bindFromRequest()

      boundedForm.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, request.user))),

        value =>
          validateOAS(value).flatMap(_.fold(
            error =>
              Future.successful(BadRequest(view(boundedForm.withGlobalError(error), mode, request.user))),
            apiName =>
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(ProduceApiEnterOasPage, value)
                    .flatMap(_.set(ProduceApiEnterApiTitlePage, apiName))
                )
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(ProduceApiEnterOasPage, mode, updatedAnswers))
          )
      ))
  }

  private def validateOAS(oas: String)(implicit messagesProvider: MessagesProvider, hc: HeaderCarrier): Future[Either[String, String]] = {
    (for {
      _ <- EitherT(applicationsConnector.validateOAS(oas)).leftMap(error => Json.prettyPrint(Json.toJson(error)))
      openApiDoc <- EitherT.fromEither(OpenApiDoc.parse(oas))
      apiName <- EitherT.fromOption(openApiDoc.getApiName(), Messages("produceApiEnterOas.error.missingApiName"))
    } yield apiName).value
  }
}
