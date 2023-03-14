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

import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.requests.{ApplicationRequest, IdentifierRequest}
import play.api.mvc.{ActionRefiner, Request, Result}
import play.api.mvc.Results._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait ApplicationAuthAction extends ActionRefiner[IdentifierRequest, ApplicationRequest]

@Singleton
class ApplicationAuthActionProvider @Inject()(
  apiHubService: ApiHubService
)(implicit val ec: ExecutionContext) {

  def apply(applicationId: String): ApplicationAuthAction = {
    new ApplicationAuthAction with FrontendHeaderCarrierProvider {
      override protected def refine[A](identifierRequest: IdentifierRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        implicit val request: Request[_] = identifierRequest

        apiHubService.getApplication(applicationId) map {
          case Some(application) =>
            identifierRequest.user.email match {
              case Some(email) if application.teamMembers.exists(teamMember => teamMember.email.equals(email)) =>
                Right(ApplicationRequest(identifierRequest, application))
              case _ =>
                Left(Redirect(routes.UnauthorisedController.onPageLoad))
            }
          case None => Left(NotFound)
        }
      }

      override protected def executionContext: ExecutionContext = ec
    }
  }

}
