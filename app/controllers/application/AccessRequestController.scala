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

package controllers.application

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import controllers.actions.IdentifierAction
import controllers.helpers.ErrorResultBuilder
import models.accessrequest.AccessRequest
import models.application.Application
import models.requests.IdentifierRequest
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.AccessRequestEndpointGroups
import views.html.admin.AccessRequestView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessRequestController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  view: AccessRequestView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(accessRequestId: String): Action[AnyContent] = identify.async {
    implicit request =>
      (for {
        accessRequest <- EitherT(fetchAccessRequest(accessRequestId))
        application <- EitherT(fetchApplication(accessRequest.applicationId))
      } yield buildView(accessRequest, application)).merge
  }

  private def fetchAccessRequest(accessRequestId: String)(implicit request: Request[?]): Future[Either[Result, AccessRequest]] = {
    apiHubService.getAccessRequest(accessRequestId).map {
      case Some(accessRequest) => Right(accessRequest)
      case None => Left(errorResultBuilder.accessRequestNotFound(accessRequestId))
    }
  }

  private def fetchApplication(applicationId: String)(implicit request: Request[?]): Future[Either[Result, Application]] = {
    apiHubService.getApplication(applicationId, enrich = false).map {
      case Some(application) => Right(application)
      case None => Left(errorResultBuilder.applicationNotFound(applicationId))
    }
  }

  private def buildView(accessRequest: AccessRequest, application: Application)(implicit request: IdentifierRequest[?]): Result = {
    Ok(view(accessRequest, application, AccessRequestEndpointGroups.group(accessRequest), ???, request.user))
  }

}
