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
import controllers.helpers.ErrorResultBuilder
import controllers.routes
import models.application.Application
import models.requests.{ApplicationRequest, IdentifierRequest}
import models.user.UserModel
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{ActionRefiner, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait ApplicationAuthAction extends ActionRefiner[IdentifierRequest, ApplicationRequest]

trait ApplicationAuthActionProvider {

  def apply(applicationId: String, enrich: Boolean = false, includeDeleted: Boolean = false)(implicit ec: ExecutionContext): ApplicationAuthAction

}

@Singleton
class ApplicationAuthActionProviderImpl @Inject()(
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  override val messagesApi: MessagesApi
) extends ApplicationAuthActionProvider with I18nSupport {

  def apply(applicationId: String, enrich: Boolean = false, includeDeleted: Boolean = false)(implicit ec: ExecutionContext): ApplicationAuthAction = {
    new ApplicationAuthAction with FrontendHeaderCarrierProvider {
      override protected def refine[A](identifierRequest: IdentifierRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        implicit val request: Request[_] = identifierRequest

        apiHubService.getApplication(applicationId, enrich, includeDeleted) map {
          case Some(application) =>
            if (identifierRequest.user.permissions.canSupport || isTeamMember(application, identifierRequest.user)) {
                Right(ApplicationRequest(identifierRequest, application))
            }
            else {
                Left(Redirect(routes.UnauthorisedController.onPageLoad))
            }
          case None =>
            Left(
              errorResultBuilder.notFound(
                Messages("site.applicationNotFoundHeading"),
                Messages("site.applicationNotFoundMessage", applicationId)
              )
            )
        }
      }

      override protected def executionContext: ExecutionContext = ec
    }
  }

  private def isTeamMember(application: Application, user: UserModel): Boolean = {
    user.email match {
      case Some(email) if application.teamMembers.exists(teamMember => teamMember.email.equals(email)) => true
      case _ => false
    }
  }

}
