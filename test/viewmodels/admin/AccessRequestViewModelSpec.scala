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

import controllers.actions.{FakeApprover, FakeUser}
import models.application.ApplicationLenses.*
import generators.{AccessRequestGenerator, ApplicationGenerator}
import models.accessrequest.{Approved, Cancelled, Pending, Rejected}
import models.user.UserModel
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers
import utils.TestHelpers

class AccessRequestViewModelSpec extends AnyFreeSpec with Matchers with ApplicationGenerator with AccessRequestGenerator with OptionValues with TestHelpers {

  private implicit val messages: Messages = Helpers.stubMessages()
  private def consumerReturnCall(applicationId: String) = controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(applicationId)
  private val consumerReturnMessage = "accessRequest.link.backForConsumer"
  private val adminReturnCall = controllers.admin.routes.AccessRequestsController.onPageLoad()
  private val adminReturnMessage = "accessRequest.link.backForAdmin"

  "consumerViewModel" - {
    "must correctly construct a Pending model when the user is a team member" in {
      forAll(teamMemberAndSupporterTable) {(user: UserModel) =>
        val application = sampleApplication().addTeamMember(user)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Pending,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = None,
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = true,
          returnCall = consumerReturnCall(application.id),
          returnMessage = consumerReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct a Pending model when the user is not a team member" in {
      forAll(nonTeamMembersOrSupport) { (user: UserModel) =>
        val application = sampleApplication().removeTeamMember(user)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Pending,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = None,
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = consumerReturnCall(application.id),
          returnMessage = consumerReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct an Approved model" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Approved,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = Some(
            AccessRequestDecisionViewModel(
              accessRequest.decision.value.decided,
              None,
              None
            )
          ),
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = consumerReturnCall(application.id),
          returnMessage = consumerReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct a Rejected model" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Rejected,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = Some(
            AccessRequestDecisionViewModel(
              accessRequest.decision.value.decided,
              None,
              accessRequest.decision.value.rejectedReason
            )
          ),
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = consumerReturnCall(application.id),
          returnMessage = consumerReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct a Cancelled model" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleCancelledAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Cancelled,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = None,
          cancelled = accessRequest.cancelled,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = consumerReturnCall(application.id),
          returnMessage = consumerReturnMessage
        )

        actual mustBe expected
      }
    }
  }

  "adminViewModel" - {
    "must correctly construct a Pending model when the user is an approver" in {
      forAll(usersWhoCanApprove) {(user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Pending,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = None,
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = true,
          canCancel = false,
          returnCall = adminReturnCall,
          returnMessage = adminReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct a Pending model when the user is not an approver" in {
      forAll(usersWhoCannotApprove) {(user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Pending,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = None,
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = adminReturnCall,
          returnMessage = adminReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct an Approved model" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Approved,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = Some(
            AccessRequestDecisionViewModel(
              accessRequest.decision.value.decided,
              Some(accessRequest.decision.value.decidedBy),
              None
            )
          ),
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = adminReturnCall,
          returnMessage = adminReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct a Rejected model" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Rejected,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = Some(
            AccessRequestDecisionViewModel(
              accessRequest.decision.value.decided,
              Some(accessRequest.decision.value.decidedBy),
              accessRequest.decision.value.rejectedReason
            )
          ),
          cancelled = None,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = adminReturnCall,
          returnMessage = adminReturnMessage
        )

        actual mustBe expected
      }
    }

    "must correctly construct a Cancelled model" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleCancelledAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        val expected = AccessRequestViewModel(
          accessRequestId = accessRequest.id,
          apiId = accessRequest.apiId,
          apiName = accessRequest.apiName,
          requested = accessRequest.requested,
          requestedBy = accessRequest.requestedBy,
          status = Cancelled,
          supportingInformation = accessRequest.supportingInformation,
          endpointGroups = AccessRequestEndpointGroups.group(accessRequest),
          decision = None,
          cancelled = accessRequest.cancelled,
          applicationId = application.id,
          applicationName = application.name,
          applicationDeleted = application.deleted,
          teamName = application.teamName,
          canDecide = false,
          canCancel = false,
          returnCall = adminReturnCall,
          returnMessage = adminReturnMessage
        )

        actual mustBe expected
      }
    }
  }

  "deletedApplicationViewModel" - {
    "must return to the deleted application page" in {
      val application = sampleApplication()
      val accessRequest = sampleCancelledAccessRequest()

      val actual = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, FakeApprover)

      actual.returnCall mustBe controllers.admin.routes.DeletedApplicationDetailsController.onPageLoad(application.id)
      actual.returnMessage mustBe "accessRequest.link.backForDeletedApplication"
    }
  }

}
