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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthorisedApproverAction, AuthorisedApproverOrSupportAction, IdentifierAction}
import controllers.helpers.{ErrorResultBuilder, Fetching}
import forms.admin.ApprovalDecisionFormProvider
import models.accessrequest.Pending
import models.requests.IdentifierRequest
import play.api.data.{Form, FormError}
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.Decision.{Approve, Reject}
import viewmodels.admin.{AccessRequestViewModel, ApprovalDecision}
import views.html.admin.{AccessRequestApprovedSuccessView, AccessRequestRejectedSuccessView, AccessRequestView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessRequestController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isApproverOrSupport: AuthorisedApproverOrSupportAction,
  isApprover: AuthorisedApproverAction,
  override val apiHubService: ApiHubService,
  override val errorResultBuilder: ErrorResultBuilder,
  formProvider: ApprovalDecisionFormProvider,
  view: AccessRequestView,
  approvedSuccessView: AccessRequestApprovedSuccessView,
  rejectedSuccessView: AccessRequestRejectedSuccessView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Fetching {

  private val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen isApproverOrSupport).async {
    implicit request =>
      buildView(id, OK, form)
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen isApprover).async {
    implicit request =>
      val boundForm = form.bindFromRequest()
      boundForm.fold(
        formWithErrors => buildView(id, BAD_REQUEST, formWithErrors),
        {
          case ApprovalDecision(Approve, _) => approve(id)
          case ApprovalDecision(Reject, Some(rejectedReason)) if rejectedReason.trim.nonEmpty => reject(id, rejectedReason)
          case _ =>
            buildView(id, BAD_REQUEST, boundForm.withError(FormError("rejectedReason", "accessRequest.rejectedReason.required")))
        }
      )
  }

  private def buildView(id: String, status: Int, form: Form[ApprovalDecision])(implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    (for{
      accessRequest <- EitherT(fetchAccessRequestOrNotFound(id))
      application <- EitherT(fetchApplicationOrNotFound(accessRequest.applicationId, includeDeleted = true))
    } yield {
      val model = AccessRequestViewModel.adminViewModel(application, accessRequest, request.user)
      Status(status)(view(model, form, request.user, false))
    }).merge
  }

  private def approve(id: String)(implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    apiHubService.approveAccessRequest(id, request.user.email).map {
      case Some(_) => Ok(approvedSuccessView(request.user))
      case _ => errorResultBuilder.accessRequestNotFound(id)
    }
  }

  private def reject(id: String, rejectedReason: String)(implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    apiHubService.rejectAccessRequest(id, request.user.email, rejectedReason.trim).map {
      case Some(_) => Ok(rejectedSuccessView(request.user))
      case _ => errorResultBuilder.accessRequestNotFound(id)
    }
  }

}
