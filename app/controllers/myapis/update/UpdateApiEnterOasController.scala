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

import cats.data.EitherT
import connectors.ApplicationsConnector
import controllers.actions.*
import controllers.myapis.update.routes
import forms.myapis.produce.ProduceApiEnterOasFormProvider
import models.Mode
import models.curl.OpenApiDoc
import navigation.Navigator
import pages.myapis.update.{UpdateApiEnterApiTitlePage, UpdateApiEnterOasPage, UpdateApiUploadOasPage}
import play.api.i18n.*
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UpdateApiSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.ProduceApiEnterOasViewModel
import views.html.myapis.produce.ProduceApiEnterOasView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiEnterOasController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: UpdateApiSessionRepository,
                                             navigator: Navigator,
                                             identify: IdentifierAction,
                                             getData: UpdateApiDataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: ProduceApiEnterOasFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: ProduceApiEnterOasView,
                                             applicationsConnector: ApplicationsConnector
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  private val form = formProvider()
  private def viewModel(mode: Mode) = ProduceApiEnterOasViewModel(
    title = "updateApiEnterOas.title",
    heading = "updateApiEnterOas.heading",
    formAction = routes.UpdateApiEnterOasController.onSubmit(mode),
    hint = "updateApiEnterOas.hint",
    populateExample = false,
  )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm =
        request.userAnswers.get(UpdateApiEnterOasPage) match {
          case Some(value) => form.fill(value)
          case _ => form.fill("")
        }

      Ok(view(preparedForm, request.user, viewModel(mode)))
  }

  def onPageLoadWithUploadedOas(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(UpdateApiEnterOasPage).orElse(request.userAnswers.get(UpdateApiUploadOasPage).map(_.fileContents)) match {
        case Some(oasFileContents) =>
          val formWithUploadedOas = form.fill(oasFileContents)
          validateOAS(oasFileContents).map {
            case Left(error) =>
              BadRequest(view(formWithUploadedOas.withGlobalError(error), request.user, viewModel(mode)))
            case Right(_) => Ok(view(formWithUploadedOas, request.user, viewModel(mode)))
          }
        case None => Future.successful(Ok(view(form, request.user, viewModel(mode))))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val boundedForm = form.bindFromRequest()

      boundedForm.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, request.user, viewModel(mode)))),

        value =>
          validateOAS(value).flatMap(_.fold(
            error =>
              Future.successful(BadRequest(view(boundedForm.withGlobalError(error), request.user, viewModel(mode)))),
            apiName =>
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(UpdateApiEnterOasPage, value)
                    .flatMap(_.set(UpdateApiEnterApiTitlePage, apiName))
                )
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(UpdateApiEnterOasPage, mode, updatedAnswers))
          )
          ))
  }

  private def validateOAS(oas: String)(implicit messagesProvider: MessagesProvider, hc: HeaderCarrier): Future[Either[String, String]] = {
    (for {
      _ <- EitherT(applicationsConnector.validateOAS(oas)).leftMap(error => Json.prettyPrint(Json.toJson(error)))
      openApiDoc <- EitherT.fromEither(OpenApiDoc.parse(oas))
      apiName <- EitherT.fromOption(openApiDoc.getApiName(), Messages("produceApiEnterOas.error.missingApiName")) //TODO
    } yield apiName).value
  }
}
