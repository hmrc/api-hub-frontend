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

package controllers.application

import config.{FrontendAppConfig, HipEnvironment, HipEnvironments}
import controllers.actions.*
import controllers.helpers.ErrorResultBuilder
import forms.AddCredentialChecklistFormProvider
import models.application.{Application, Credential}
import models.exception.ApplicationCredentialLimitException
import models.requests.{ApplicationRequest, BaseRequest}
import models.user.UserModel
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.AddCredentialSuccessViewModel
import views.html.application.{AddCredentialChecklistView, AddCredentialSuccessView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddCredentialController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  isPrivileged: AuthorisedPrivilegedUserAction,
  applicationAuth: ApplicationAuthActionProvider,
  formProvider: AddCredentialChecklistFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AddCredentialChecklistView,
  errorResultBuilder: ErrorResultBuilder,
  apiHubService: ApiHubService,
  successView: AddCredentialSuccessView,
  hipEnvironments: HipEnvironments,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  private val credentialsTabFragment = "#credentials"

  def checklist(applicationId: String): Action[AnyContent] = (identify andThen isPrivileged andThen applicationAuth(applicationId)) {
    implicit request =>
      Ok(view(form, applicationId, request.maybeUser))
  }

  def addCredentialForEnvironment(applicationId: String, environment: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)).async {
    implicit request =>
      hipEnvironments.forUrlPathParameter(environment) match {
        case hipEnvironment if hipEnvironment.isProductionLike =>
          form.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, applicationId, request.maybeUser))),
            _ => addCredentialToProduction(hipEnvironment)
          )
        case hipEnvironment => addCredentialToNonProduction(hipEnvironment)
      }
  }

  private def addCredentialToNonProduction(hipEnvironment: HipEnvironment)(implicit request: ApplicationRequest[AnyContent]) = {
    addCredential(
      hipEnvironment,
      credential => Future.successful(SeeOther(controllers.application.routes.EnvironmentsController.onPageLoad(request.application.id, hipEnvironment.id).url + credentialsTabFragment))
    )
  }

  private def addCredentialToProduction(hipEnvironment: HipEnvironment)(implicit request: ApplicationRequest[AnyContent]) = {
    isPrivileged.invokeBlock(
      request.identifierRequest,
      _ => addCredential(
        hipEnvironment,
        credential => fetchApiNames(request.application).map(
          apiNames =>
            addCredentialSuccess(request.application, apiNames, credential, request.identifierRequest.user, hipEnvironment)
        )
      )
    )
  }

  private def addCredential(hipEnvironment: HipEnvironment, onSuccess: Credential => Future[Result])(implicit request: ApplicationRequest[AnyContent]) = {
    apiHubService.addCredential(request.application.id, hipEnvironment) flatMap {
      case Right(Some(credential)) => onSuccess(credential)
      case Right(None) => applicationNotFound(request.application)
      case Left(_: ApplicationCredentialLimitException) => tooManyCredentials(request.application)
      case Left(e) => Future.successful(errorResultBuilder.internalServerError(e))
    }
  }

  private def fetchApiNames(application: Application)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    Future.sequence(
      application.apis.map(
        api =>
          apiHubService
            .getApiDetail(api.id)
            .map {
              case Some(apiDetail) => Some(apiDetail.title)
              case _ => None
            }
      )
    ).map(_.flatten)
  }

  private def addCredentialSuccess(
    application: Application,
    apiNames: Seq[String],
    credential: Credential,
    user: UserModel,
    hipEnvironment: HipEnvironment
  )(implicit request: Request[?]): Result = {
    val summaryList = AddCredentialSuccessViewModel.buildSummary(
      application,
      apiNames,
      credential
    )

    Ok(successView(application, summaryList, Some(user), credential, controllers.application.routes.EnvironmentsController.onPageLoad(application.id, hipEnvironment.id).url + credentialsTabFragment))
  }

  private def applicationNotFound(application: Application)(implicit request: BaseRequest[?]): Future[Result] = {
    Future.successful(
      errorResultBuilder.notFound(
        heading = Messages("site.applicationNotFoundHeading"),
        message = Messages("site.applicationNotFoundMessage", application.id)
      )
    )
  }

  private def tooManyCredentials(application: Application)(implicit request: BaseRequest[?]): Future[Result] = {
    val linkUrl = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id).url
    val link = s"<a class=\"govuk-link govuk-link--no-visited-state\" href=\"$linkUrl\">${application.name}</a>"

    Future.successful(
      errorResultBuilder.badRequest(
        heading = Messages("addCredentialSuccess.tooManyCredentials.heading"),
        message = Messages("addCredentialSuccess.tooManyCredentials.message", link)
      )
    )
  }

}
