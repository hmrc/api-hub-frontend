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

package controllers.admin

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthorisedApproverAction, AuthorisedApproverOrSupportAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.AccessRequestEndpointGroups
import views.html.admin.AccessRequestView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessRequestController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isApproverOrSupport: AuthorisedApproverOrSupportAction,
  isApprover: AuthorisedApproverAction,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  view: AccessRequestView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen isApproverOrSupport).async {
    implicit request =>
      apiHubService.getAccessRequest(id).flatMap {
        case Some(accessRequest) =>
          apiHubService.getApplication(accessRequest.applicationId, enrich = false).map {
            case Some(application) => Ok(view(accessRequest, application, AccessRequestEndpointGroups.group(accessRequest), request.user))
            case _ => applicationNotFound(accessRequest.applicationId)
          }
        case None => Future.successful(accessRequestNotFound(id))
      }
  }

  def approve(id: String): Action[AnyContent] = (identify andThen isApprover).async {
    implicit request =>
      request.user.email match {
        case Some(email) =>
          apiHubService.approveAccessRequest(id, email).map {
            case Some(_) => Redirect(controllers.admin.routes.AccessRequestsController.onPageLoad())
            case _ => accessRequestNotFound(id)
          }
        case _ => noEmail()
      }
  }

  private def accessRequestNotFound(accessRequestId: String)(implicit request: Request[_]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.accessRequestNotFound.heading"),
      message = Messages("site.accessRequestNotFound.message", accessRequestId)
    )
  }

  private def applicationNotFound(applicationId: String)(implicit request: Request[_]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.applicationNotFoundHeading"),
      message = Messages("site.applicationNotFoundMessage", applicationId)
    )
  }

  private def noEmail()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.internalServerError("The current user does not have an email address")
    )
  }

}
