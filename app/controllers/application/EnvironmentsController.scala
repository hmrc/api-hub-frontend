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
import controllers.helpers.ErrorResultBuilder
import models.application.*
import models.exception.ApplicationCredentialLimitException
import models.user.Permissions
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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
)(implicit ec: ExecutionContext, hipEnvironments: HipEnvironments) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String, environment: String): Action[AnyContent] = (identify andThen applicationAuth(id)).async {
    implicit request =>
      hipEnvironments.environments.find(_.id == environment)
        .map(hipEnvironment =>
          retrieveCredentials(request.application, hipEnvironment)
            .map {
              case Left(_) =>
                Ok(view(request.application, request.identifierRequest.user, hipEnvironment, errorRetrievingCredentials = true))
              case Right(credentials) =>
                Ok(view(request.application, request.identifierRequest.user, hipEnvironment, credentials))
            } 
        ).getOrElse(
          Future.successful(errorResultBuilder.environmentNotFound(environment))
        )
  }

  private def retrieveCredentials(
                                   application: Application,
                                   hipEnvironment: HipEnvironment
                                 )(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[Seq[Credential]]]] =
    if (!hipEnvironment.isProductionLike) then
      apiHubService.fetchCredentials(application.id, hipEnvironment)
    else Future.successful(Right(
      Some(application.environments.primary.credentials)
    ))

}
