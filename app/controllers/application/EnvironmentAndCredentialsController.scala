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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.application.{EnvironmentName, Primary, Secondary}
import models.exception.ApplicationCredentialLimitException
import models.user.Permissions
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.EnvironmentAndCredentialsView

import scala.concurrent.{ExecutionContext, Future}

class EnvironmentAndCredentialsController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: EnvironmentAndCredentialsView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)) {
    implicit request =>
      Ok(view(request.application, request.identifierRequest.user, config.helpDocsPath))
  }

  def deletePrimaryCredential(id: String, clientId: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      request.identifierRequest.user.permissions match {
        case Permissions(_, true, _) | Permissions(_, _, true) =>
          val url = s"${controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(id).url}#hip-production"
          deleteCredential(id, clientId, Primary, url)
        case _ =>
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
      }
  }

  def deleteSecondaryCredential(id: String, clientId: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      val url = s"${controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(id).url}#hip-development"
      deleteCredential(id, clientId, Secondary, url)
  }

  private def deleteCredential(id: String, clientId: String, environmentName: EnvironmentName, url: String)(implicit request: Request[?]) = {
    apiHubService.deleteCredential(id, environmentName, clientId).map {
      case Right(Some(())) => Redirect(url)
      case Right(None) => credentialNotFound(id, clientId)
      case Left(_: ApplicationCredentialLimitException) => lastCredential()
      case Left(e) => throw e
    }
  }

  private def credentialNotFound(applicationId: String, clientId: String)(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("environmentAndCredentials.credentialNotFound.heading"),
      message = Messages("environmentAndCredentials.credentialNotFound.message", clientId, applicationId)
    )
  }

  private def lastCredential()(implicit request: Request[?]): Result = {
    errorResultBuilder.badRequest(
      heading = Messages("environmentAndCredentials.cannotDeleteLastCredential.heading"),
      message = Messages("environmentAndCredentials.cannotDeleteLastCredential.message")
    )
  }

}
