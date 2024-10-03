package viewmodels.admin

import controllers.actions.FakeUser
import models.application.ApplicationLenses.*
import generators.{AccessRequestGenerator, ApplicationGenerator}
import models.accessrequest.{Approved, Cancelled, Pending, Rejected}
import models.user.UserModel
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import utils.TestHelpers

class AccessRequestViewModelSpec extends AnyFreeSpec with Matchers with ApplicationGenerator with AccessRequestGenerator with OptionValues with TestHelpers {

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
          canCancel = true
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
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
          canCancel = false
        )

        actual mustBe expected
      }
    }
  }

}
