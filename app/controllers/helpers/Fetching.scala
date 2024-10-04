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

package controllers.helpers

import models.accessrequest.AccessRequest
import models.application.Application
import play.api.mvc.{Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait Fetching {
  self: FrontendBaseController =>

  def errorResultBuilder: ErrorResultBuilder
  def apiHubService: ApiHubService

  def fetchAccessRequestOrNotFound(accessRequestId: String)(implicit request: Request[?], ec: ExecutionContext): Future[Either[Result, AccessRequest]] = {
    apiHubService.getAccessRequest(accessRequestId).map {
      case Some(accessRequest) => Right(accessRequest)
      case None => Left(errorResultBuilder.accessRequestNotFound(accessRequestId))
    }
  }

  def fetchApplicationOrNotFound(applicationId: String, enrich: Boolean = false, includeDeleted: Boolean = false)(implicit request: Request[?], ec: ExecutionContext): Future[Either[Result, Application]] = {
    apiHubService.getApplication(applicationId, enrich, includeDeleted).map {
      case Some(application) => Right(application)
      case None => Left(errorResultBuilder.applicationNotFound(applicationId))
    }
  }

}
