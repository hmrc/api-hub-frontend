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

package viewmodels.admin

import models.accessrequest.{AccessRequest, AccessRequestCancelled, AccessRequestDecision, AccessRequestStatus, Pending}
import models.application.{Application, Deleted}
import models.application.ApplicationLenses.*
import models.user.UserModel

import java.time.LocalDateTime

case class AccessRequestDecisionViewModel(decided: LocalDateTime, decidedBy: Option[String], rejectedReason: Option[String])

object AccessRequestDecisionViewModel {

  def consumerViewModel(decision: AccessRequestDecision): AccessRequestDecisionViewModel = {
    AccessRequestDecisionViewModel(
      decided = decision.decided,
      decidedBy = None,
      rejectedReason = decision.rejectedReason
    )
  }

  def adminViewModel(decision: AccessRequestDecision): AccessRequestDecisionViewModel = {
    AccessRequestDecisionViewModel(
      decided = decision.decided,
      decidedBy = Some(decision.decidedBy),
      rejectedReason = decision.rejectedReason
    )
  }

}

case class AccessRequestViewModel(
  accessRequestId: String,
  apiId: String,
  apiName: String,
  requested: LocalDateTime,
  requestedBy: String,
  status: AccessRequestStatus,
  supportingInformation: String,
  endpointGroups: Seq[AccessRequestEndpointGroup],
  decision: Option[AccessRequestDecisionViewModel],
  cancelled: Option[AccessRequestCancelled],
  applicationId: String,
  applicationName: String,
  applicationDeleted: Option[Deleted],
  teamName: Option[String],
  canDecide: Boolean,
  canCancel: Boolean
)

object AccessRequestViewModel {

  private enum ViewType:
    case Consumer, Admin

  def consumerViewModel(application: Application, accessRequest: AccessRequest, user: UserModel): AccessRequestViewModel = {
    apply(application, accessRequest, user, ViewType.Consumer)
  }

  def adminViewModel(application: Application, accessRequest: AccessRequest, user: UserModel): AccessRequestViewModel = {
    apply(application, accessRequest, user, ViewType.Admin)
  }

  private def apply(application: Application, accessRequest: AccessRequest, user: UserModel, viewType: ViewType): AccessRequestViewModel = {
    AccessRequestViewModel(
      accessRequestId = accessRequest.id,
      apiId = accessRequest.apiId,
      apiName = accessRequest.apiName,
      requested = accessRequest.requested,
      requestedBy = accessRequest.requestedBy,
      status = accessRequest.status,
      supportingInformation = accessRequest.supportingInformation,
      endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
      decision = buildDecision(accessRequest, viewType),
      cancelled = accessRequest.cancelled,
      applicationId = application.id,
      applicationName = application.name,
      applicationDeleted = application.deleted,
      teamName = application.teamName,
      canDecide = canDecide(accessRequest, user, viewType),
      canCancel = canCancel(application, accessRequest, user, viewType)
    )
  }

  private def buildDecision(accessRequest: AccessRequest, viewType: ViewType): Option[AccessRequestDecisionViewModel] = {
    accessRequest.decision.map(
      decision =>
        if (viewType == ViewType.Consumer) {
          AccessRequestDecisionViewModel.consumerViewModel(decision)
        }
        else {
          AccessRequestDecisionViewModel.adminViewModel(decision)
        }
    )
  }

  private def canDecide(accessRequest: AccessRequest, user: UserModel, viewType: ViewType): Boolean = {
    viewType == ViewType.Admin
      && accessRequest.status == Pending
      && user.permissions.canApprove
  }

  private def canCancel(application: Application, accessRequest: AccessRequest, user: UserModel, viewType: ViewType): Boolean = {
    viewType == ViewType.Consumer
      && accessRequest.status == Pending
      && application.isAccessible(user)
  }

}
