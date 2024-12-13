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
import config.{FrontendAppConfig, HipEnvironment, HipEnvironments}
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.{ApplicationApiBuilder, ErrorResultBuilder}
import models.application.*
import models.application.ApplicationLenses.*
import models.exception.ApplicationCredentialLimitException
import models.requests.ApplicationRequest
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.application.EnvironmentsViewModel
import views.html.application.EnvironmentsView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EnvironmentsController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: EnvironmentsView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  config: FrontendAppConfig,
  hipEnvironments: HipEnvironments,
  applicationApiBuilder: ApplicationApiBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String, environment: String): Action[AnyContent] = (identify andThen applicationAuth(id)).async {
    implicit request =>
      hipEnvironments.environments.find(_.id == environment)
        .map(hipEnvironment =>
          for {
            (credentials, hasFailedRetrieval) <- retrieveCredentials(request.application, hipEnvironment)
            applicationApis <- applicationApiBuilder.build(request.application)  
          } yield Ok(view(EnvironmentsViewModel(request.application, applicationApis, request.identifierRequest.user, hipEnvironment, credentials, config.helpDocsPath, hasFailedRetrieval)))
        ).getOrElse(
          Future.successful(errorResultBuilder.environmentNotFound(environment))
        )
  }

  def onDeleteCredential(id: String, clientId: String, environmentId: String): Action[AnyContent] = (identify andThen applicationAuth(id)).async {
    implicit request =>
      hipEnvironments.environments.find(_.id == environmentId)
        .map { hipEnvironment =>
          val url = s"${routes.EnvironmentsController.onPageLoad(id, environmentId).url}#credentials"
          deleteCredential(id, clientId, hipEnvironment, url)
        }.getOrElse(
          Future.successful(errorResultBuilder.environmentNotFound(environmentId))
        )
  }

  private def retrieveCredentials(
                                   application: Application,
                                   hipEnvironment: HipEnvironment
                                 )(implicit hc: HeaderCarrier): Future[(Seq[Credential], Boolean)] =
    val applicationEnvironmentCredentials = application.getCredentials(hipEnvironment)
    if (!hipEnvironment.isProductionLike) then
      apiHubService.fetchCredentials(application.id, hipEnvironment)
        .map ({
            case Some(credentials) => (credentials, false)
            case _ => (applicationEnvironmentCredentials, false)
          })
        .recover {
          case NonFatal(_) => (applicationEnvironmentCredentials, true)
        }
    else Future.successful((applicationEnvironmentCredentials, false))

  private def deleteCredential(id: String, clientId: String, hipEnvironment: HipEnvironment, url: String)(implicit request: ApplicationRequest[?]) = {
    if (!hipEnvironment.isProductionLike || request.identifierRequest.user.permissions.isPrivileged) {
      apiHubService.deleteCredential(id, hipEnvironment, clientId).map {
        case Right(Some(())) => Redirect(url)
        case Right(None) => credentialNotFound(id, clientId)
        case Left(_: ApplicationCredentialLimitException) => lastCredential()
        case Left(e) => throw e
      }
    } else {
      Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
    }
  }

  private def credentialNotFound(applicationId: String, clientId: String)(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("environments.credentialNotFound.heading"),
      message = Messages("environments.credentialNotFound.message", clientId, applicationId)
    )
  }

  private def lastCredential()(implicit request: Request[?]): Result = {
    errorResultBuilder.badRequest(
      heading = Messages("environments.cannotDeleteLastCredential.heading"),
      message = Messages("environments.cannotDeleteLastCredential.message")
    )
  }

}
