/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import config.FrontendAppConfig
import controllers.actions._
import controllers.helpers.ErrorResultBuilder
import forms.ProductionCredentialsChecklistFormProvider
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Application, Credential, Secret}
import models.requests.ApplicationRequest
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.GeneratePrimarySecretSuccessViewModel
import views.html.{GeneratePrimarySecretSuccessView, ProductionCredentialsChecklistView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProductionCredentialsChecklistController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  formProvider: ProductionCredentialsChecklistFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ProductionCredentialsChecklistView,
  errorResultBuilder: ErrorResultBuilder,
  apiHubService: ApiHubService,
  frontendAppConfig: FrontendAppConfig,
  successView: GeneratePrimarySecretSuccessView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(applicationId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)).async {
    implicit request =>
      validatePrimaryCredential(request.application).fold(
        result => result,
        _ => Future.successful(Ok(view(form, applicationId)))
      )
  }

  def onSubmit(applicationId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, applicationId))),
        _ =>
          validatePrimaryCredential(request.application).fold(
            result => result,
            credential => apiHubService.createPrimarySecret(request.application.id) map {
              case Some(secret) => generatePrimarySecretSuccess(request, credential, secret)
              case _ => credentialNotFound()
            }
          )
      )
  }

  private def validatePrimaryCredential(application: Application)(implicit request: Request[_]): Either[Future[Result], Credential] = {
    application.getPrimaryCredentials match {
      case Seq(credential @ Credential(_, _, _, None)) => Right(credential)
      case Seq(Credential(_, _, _, Some(_))) => Left(secretAlreadyGenerated())
      case _ => Left(invalidCredential())
    }
  }

  private def generatePrimarySecretSuccess(request: ApplicationRequest[_], credential: Credential, secret: Secret): Result = {
    implicit val implicitRequest: Request[_] = request

    val summaryList = GeneratePrimarySecretSuccessViewModel.buildSummary(
      request.application,
      frontendAppConfig.environmentNames,
      credential,
      secret
    )

    Ok(successView(request.application, summaryList, Some(request.identifierRequest.user), secret.secret))
  }

  private def invalidCredential()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.badRequest(
        Messages("generatePrimarySecretSuccess.invalidCredential.heading"),
        Messages("generatePrimarySecretSuccess.invalidCredential.message")
      )
    )
  }

  private def secretAlreadyGenerated()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.badRequest(
        Messages("generatePrimarySecretSuccess.alreadyGenerated.heading"),
        Messages("generatePrimarySecretSuccess.alreadyGenerated.message")
      )
    )
  }

  private def credentialNotFound()(implicit request: Request[_]): Result = {
    errorResultBuilder.notFound(
      Messages("generatePrimarySecretSuccess.notFound.heading"),
      Messages("generatePrimarySecretSuccess.notFound.message")
    )
  }

}
