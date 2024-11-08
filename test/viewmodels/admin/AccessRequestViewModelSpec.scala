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

import controllers.actions.FakeUser
import generators.{AccessRequestGenerator, ApplicationGenerator}
import models.accessrequest.Pending
import models.application.ApplicationLenses.*
import models.user.UserModel
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers
import utils.TestHelpers

class AccessRequestViewModelSpec extends AnyFreeSpec with Matchers with ApplicationGenerator with AccessRequestGenerator with OptionValues with TestHelpers {

  import AccessRequestViewModelSpec.*

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
          returnMessage = consumerReturnMessage,
          activeLink = None
        )

        actual mustBe expected
      }
    }

    "must not allow non team members to cancel a pending access request" in {
      forAll(nonTeamMembersOrSupport) { (user: UserModel) =>
        val application = sampleApplication().removeTeamMember(user)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject a pending access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel an approved access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must correctly construct the approved decision model for a non-admin, hiding approved by" in {
      forAll(usersWhoCannotViewApprovals) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestDecisionViewModel(
          decided = accessRequest.decision.value.decided,
          decidedBy = None,
          rejectedReason = None
        )

        actual.decision.value mustBe expected
      }
    }

    "must correctly construct the approved decision attributes for an admin, showing approved by" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestDecisionViewModel(
          decided = accessRequest.decision.value.decided,
          decidedBy = Some(accessRequest.decision.value.decidedBy),
          rejectedReason = None
        )

        actual.decision.value mustBe expected
      }
    }

    "must not allow any user to approve/reject/cancel a rejected access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must correctly construct the rejected decision attributes for a user, hiding approved by" in {
      forAll(usersWhoCannotViewApprovals) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestDecisionViewModel(
          decided = accessRequest.decision.value.decided,
          decidedBy = None,
          rejectedReason = accessRequest.decision.value.rejectedReason
        )

        actual.decision.value mustBe expected
      }
    }

    "must correctly construct the rejected decision attributes for an admin, showing approved by" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        val expected = AccessRequestDecisionViewModel(
          decided = accessRequest.decision.value.decided,
          decidedBy = Some(accessRequest.decision.value.decidedBy),
          rejectedReason = accessRequest.decision.value.rejectedReason
        )

        actual.decision.value mustBe expected
      }
    }

    "must not allow any user to approve/reject/cancel a cancelled access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleCancelledAccessRequest()

        val actual = AccessRequestViewModel.consumerViewModel(application, accessRequest, user)

        actual.canCancel mustBe false
        actual.canDecide mustBe false
      }
    }
  }

  "adminViewModel" - {
    "must return to the admin view" in {
      val application = sampleApplication()
      val accessRequest = sampleAccessRequest()

      val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, FakeUser)

      actual.returnCall mustBe adminReturnCall
      actual.returnMessage mustBe adminReturnMessage
    }

    "must allow an approver to approve/reject a pending access request" in {
      forAll(usersWhoCanApprove) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        actual.canDecide mustBe true
      }
    }

    "must not allow a non-approver to approve/reject a pending access request" in {
      forAll(usersWhoCannotApprove) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
      }
    }

    "must not allow any user to cancel a pending access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel an approved access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel a rejected access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel a cancelled access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleCancelledAccessRequest()

        val actual = AccessRequestViewModel.adminViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }
  }

  "deletedApplicationViewModel" - {
    "must return to the deleted application page" in {
      val application = sampleApplication()
      val accessRequest = sampleAccessRequest()

      val actual = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, FakeUser)

      actual.returnCall mustBe deletedApplicationCall(application.id)
      actual.returnMessage mustBe deletedApplicationMessage
    }

    "must not allow any user to approve/reject/cancel a pending access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = samplePendingAccessRequest()

        val actual = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel an approved access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleApprovedAccessRequest()

        val actual = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel a rejected access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleRejectedAccessRequest()

        val actual = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }

    "must not allow any user to approve/reject/cancel a cancelled access request" in {
      forAll(allUsers) { (user: UserModel) =>
        val application = sampleApplication().addTeamMember(FakeUser.email)
        val accessRequest = sampleCancelledAccessRequest()

        val actual = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, user)

        actual.canDecide mustBe false
        actual.canCancel mustBe false
      }
    }
  }

}

object AccessRequestViewModelSpec {

  private implicit val messages: Messages = Helpers.stubMessages()

  private def consumerReturnCall(applicationId: String) = controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(applicationId)
  private val consumerReturnMessage = "accessRequest.link.backForConsumer"

  private val adminReturnCall = controllers.admin.routes.AccessRequestsController.onPageLoad()
  private val adminReturnMessage = "accessRequest.link.backForAdmin"

  private def deletedApplicationCall(applicationId: String) = controllers.admin.routes.DeletedApplicationDetailsController.onPageLoad(applicationId)
  private val deletedApplicationMessage = "accessRequest.link.backForDeletedApplication"

}
