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

package controllers.actions

import com.google.inject.Inject
import connectors.ApplicationsConnector
import handlers.ErrorHandler
import models.requests.{ApiRequest, IdentifierRequest, StatusRequest}
import models.user.{LdapUser, StrideUser, UserModel, UserType}
import play.api.Logging
import play.api.mvc.Results.*
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait StatusAction extends ActionRefiner[IdentifierRequest, StatusRequest]

trait StatusActionProvider {
  def apply()(implicit ec: ExecutionContext): StatusAction
}

class StatusActionProviderImpl @Inject()(
  applicationsConnector: ApplicationsConnector,
  errorHandler: ErrorHandler
)(implicit val executionContext: ExecutionContext) extends StatusActionProvider {

  def apply()(implicit ec: ExecutionContext): StatusAction =
    new StatusAction with FrontendHeaderCarrierProvider {
      override protected def refine[A](identifierRequest: IdentifierRequest[A]): Future[Either[Result, StatusRequest[A]]] =
        implicit val request: Request[?] = identifierRequest
        applicationsConnector.status().map(statuses =>
          Right(StatusRequest(identifierRequest, identifierRequest.user, statuses))
        )

      override protected def executionContext: ExecutionContext = ec
    }

}
