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

package controllers.admin

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthorisedApproverOrSupportAction, IdentifierAction}
import controllers.helpers.{ErrorResultBuilder, Fetching}
import forms.admin.ApprovalDecisionFormProvider
import models.accessrequest.{AccessRequest, Pending}
import models.application.Application
import models.requests.IdentifierRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.AccessRequestViewModel
import views.html.admin.AccessRequestView

import scala.concurrent.ExecutionContext

@Singleton
class DeletedApplicationAccessRequestController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isApproverOrSupport: AuthorisedApproverOrSupportAction,
  override val apiHubService: ApiHubService,
  override val errorResultBuilder: ErrorResultBuilder,
  formProvider: ApprovalDecisionFormProvider,
  view: AccessRequestView,
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Fetching {

  private val form = formProvider()

  def onPageLoad(accessRequestId: String): Action[AnyContent] = (identify andThen isApproverOrSupport).async {
    implicit request =>
      (for {
        accessRequest <- EitherT(fetchAccessRequestOrNotFound(accessRequestId))
        application <- EitherT(fetchApplicationOrNotFound(accessRequest.applicationId, includeDeleted = true))
      } yield buildView(accessRequest, application)).merge
  }

  private def buildView(accessRequest: AccessRequest, application: Application)(implicit request: IdentifierRequest[?]): Result = {
    val model = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, request.user)
    val isUserTeamMember = application.teamMembers.exists(_.email.equalsIgnoreCase(request.user.email))
    val allowAccessRequestCancellation = accessRequest.status == Pending && isUserTeamMember
    Ok(view(model, form, request.user, allowAccessRequestCancellation))
  }

}
