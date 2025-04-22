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

package services

import config.{BaseHipEnvironment, ShareableHipConfig}
import connectors.{ApimConnector, ApplicationsConnector, IntegrationCatalogueConnector}
import controllers.actions.FakeApplication
import fakes.{FakeHipEnvironments, FakePlatforms}
import generators.{AccessRequestGenerator, ApiDetailGenerators, EgressGenerator}
import models.AvailableEndpoint
import models.accessrequest.*
import models.api.ApiDeploymentStatus.*
import models.api.ApiDetailLensesSpec.sampleApiDetailSummary
import models.api.{ApiDeploymentStatuses, ApiDetail, ContactInfo, EndpointMethod, IntegrationPlatformReport, PlatformContact}
import models.application.*
import models.application.ApplicationLenses.*
import models.deployment.{DeploymentDetails, DeploymentsRequest, EgressMapping, SuccessfulDeploymentsResponse}
import models.requests.{AddApiRequest, AddApiRequestEndpoint}
import models.stats.{ApisInProductionStatistic, DashboardStatistics, DashboardStatisticsBuilder}
import models.team.{NewTeam, Team}
import models.user.UserContactDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.admin.ApimRequest

import java.time.LocalDateTime
import scala.concurrent.Future

class ApiHubServiceSpec
  extends AsyncFreeSpec
    with OptionValues
    with EitherValues
    with ApplicationGetterBehaviours
    with ApiDetailGenerators
    with AccessRequestGenerator
    with EgressGenerator
    with TableDrivenPropertyChecks {

  "registerApplication" - {
    "must call the applications connector and return the saved application" in {
      val newApplication = NewApplication("test-app-name", Creator("test-creator-email"), Seq(TeamMember("test-creator-email")))
      val expected = Application("id", newApplication)

      val fixture = buildFixture()
      when(fixture.applicationsConnector.registerApplication(eqTo(newApplication))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.registerApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).registerApplication(eqTo(newApplication))(any())
          succeed
      }
    }
  }

  "getApplications" - {
    "must call the applications connector and return a sequence of applications" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val fixture = buildFixture()
      when(fixture.applicationsConnector.getApplications(any(), any())(any())).thenReturn(Future.successful(expected))

      fixture.service.getApplications(None, false)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).getApplications(eqTo(None), eqTo(false))(any())
          succeed
      }
    }

    "must call the applications connector and return a sequence of applications including deleted when requested" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
        .delete(Deleted(LocalDateTime.now(), "test-deleted-by"))
      val expected = Seq(application1, application2)

      val fixture = buildFixture()
      when(fixture.applicationsConnector.getApplications(any(), any())(any())).thenReturn(Future.successful(expected))

      fixture.service.getApplications(None, true)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).getApplications(eqTo(None), eqTo(true))(any())
          succeed
      }
    }

    "must call the applications connector and return a user's applications when requested" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val fixture = buildFixture()

      when(fixture.applicationsConnector.getApplications(eqTo(Some("test-creator-email-2")), eqTo(false))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getApplications(Some("test-creator-email-2"), false)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).getApplications(eqTo(Some("test-creator-email-2")), eqTo(false))(any())
          succeed
      }
    }
  }
  "getApplicationsUsingApi" - {
    val apiId = "myApiId"
    val apiTitle = "myApiTitle"

    "must call the applications connector and return a sequence of applications" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1"))).addApi(Api(apiId, apiTitle))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2"))).addApi(Api(apiId, apiTitle))
      val expected = Seq(application1, application2)

      val fixture = buildFixture()
      when(fixture.applicationsConnector.getApplicationsUsingApi(any(), any())(any())).thenReturn(Future.successful(expected))

      fixture.service.getApplicationsUsingApi(apiId, false)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).getApplicationsUsingApi(eqTo(apiId), eqTo(false))(any())
          succeed
      }
    }

    "must call the applications connector and return a sequence of applications including deleted when requested" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1"))).addApi(Api(apiId, apiTitle))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2"))).addApi(Api(apiId, apiTitle))
        .delete(Deleted(LocalDateTime.now(), "test-deleted-by"))
      val expected = Seq(application1, application2)

      val fixture = buildFixture()
      when(fixture.applicationsConnector.getApplicationsUsingApi(any(), any())(any())).thenReturn(Future.successful(expected))

      fixture.service.getApplicationsUsingApi(apiId, true)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).getApplicationsUsingApi(eqTo(apiId), eqTo(true))(any())
          succeed
      }
    }
  }

  "getApplication" - {
    "must" - {
      behave like successfulApplicationGetter(false)
    }
  }

  "deleteApplication" - {
    "must delete the application via the applications connector for the specified user" in {
      val id = "test-id"

      val userEmail = Some("me@test.com")
      val fixture = buildFixture()
      when(fixture.applicationsConnector.deleteApplication(eqTo(id), eqTo(userEmail))(any()))
        .thenReturn(Future.successful(Some(())))

      fixture.service.deleteApplication(id, userEmail)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
          verify(fixture.applicationsConnector).deleteApplication(any(), eqTo(userEmail))(any())
          succeed
      }
    }

    "must return None when the applications connectors does to indicate the application was not found" in {
      val id = "test-id"

      val fixture = buildFixture()
      when(fixture.applicationsConnector.deleteApplication(eqTo(id), any())(any()))
        .thenReturn(Future.successful(None))

      fixture.service.deleteApplication(id, None)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "testConnectivity" - {
    "must call the applications connector and return something" in {
      val expected = "something"
      val fixture = buildFixture()

      when(fixture.applicationsConnector.testConnectivity()(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.testConnectivity()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "getApiDetail" - {
    "must call the integration catalogue connector and return the API detail" in {
      val expected = sampleApiDetail()

      val fixture = buildFixture()

      when(fixture.integrationCatalogueConnector.getApiDetail(eqTo(expected.id))(any()))
        .thenReturn(Future.successful(Some(expected)))

      fixture.service.getApiDetail(expected.id)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }
  }

  "getApiDetailForPublishReference" - {
    "must call the integration catalogue connector and return the API detail" in {
      val expected = sampleApiDetail()

      val fixture = buildFixture()

      when(fixture.integrationCatalogueConnector.getApiDetailForPublishReference(eqTo(expected.publisherReference))(any()))
        .thenReturn(Future.successful(Some(expected)))

      fixture.service.getApiDetailForPublishReference(expected.publisherReference)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }
  }

  "getApiDeploymentStatuses" - {
    "must call the applications connector and return the API detail" in {
      val publisherReference = "ref123"
      val expected = ApiDeploymentStatuses(Seq(
        Deployed(FakeHipEnvironments.production.id, "1"),
        Deployed(FakeHipEnvironments.test.id, "1")
      ))

      val fixture = buildFixture()

      when(fixture.applicationsConnector.getApiDeploymentStatuses(eqTo(publisherReference))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getApiDeploymentStatuses(publisherReference)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "getApiDeploymentStatus" - {
    "must call the applications connector and return the API detail" in {
      val fixture = buildFixture()
      val hipEnvironment = FakeHipEnvironments.production
      val publisherReference = "ref123"
      val expected = Deployed(hipEnvironment.id, "1.0.1")

      when(fixture.applicationsConnector.getApiDeploymentStatus(any, any)(any))
        .thenReturn(Future.successful(expected))

      fixture.applicationsConnector.getApiDeploymentStatus(hipEnvironment, publisherReference)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).getApiDeploymentStatus(eqTo(hipEnvironment), eqTo(publisherReference))(any)
          result mustBe expected
      }
    }
  }

  "getDeploymentDetails" - {
    "must call the applications connector and return the deployment detail" in {
      val publisherReference = "ref123"

      val deploymentDetails = DeploymentDetails(
        description = Some("test-description"),
        status = Some("test-status"),
        domain = Some("test-domain"),
        subDomain = Some("test-dub-domain"),
        hods = Some(Seq("test-backend-1", "test-backend-2")),
        egressMappings = Some(Seq(EgressMapping("prefix", "egress-prefix"))),
        prefixesToRemove = Some(Seq("test-prefix-1", "test-prefix-2")),
        egress = Some("test-egress"),
      )

      val fixture = buildFixture()

      when(fixture.applicationsConnector.getDeploymentDetails(eqTo(publisherReference), any())(any()))
        .thenReturn(Future.successful(Some(deploymentDetails)))

      fixture.service.getDeploymentDetails(publisherReference)(HeaderCarrier()).map(
        actual =>
          actual.value mustBe deploymentDetails
      )
    }

    "must call the applications connector and return the deployment detail for a specific environment" in {
      val publisherReference = "ref123"

      val deploymentDetails = DeploymentDetails(
        description = Some("test-description"),
        status = Some("test-status"),
        domain = Some("test-domain"),
        subDomain = Some("test-dub-domain"),
        hods = Some(Seq("test-backend-1", "test-backend-2")),
        egressMappings = Some(Seq(EgressMapping("prefix", "egress-prefix"))),
        prefixesToRemove = Some(Seq("test-prefix-1", "test-prefix-2")),
        egress = Some("test-egress"),
      )
      val environment = FakeHipEnvironments.production

      val fixture = buildFixture()

      when(fixture.applicationsConnector.getDeploymentDetails(eqTo(publisherReference), eqTo(Some(environment)))(any()))
        .thenReturn(Future.successful(Some(deploymentDetails)))

      fixture.service.getDeploymentDetails(publisherReference, environment)(HeaderCarrier()).map(
        actual =>
          actual.value mustBe deploymentDetails
      )
    }
  }

  "getApis" - {
    "must call the integration catalogue connector and return API details if no filter specified" in {
      val expected = Seq(sampleApiDetailSummary())

      val fixture = buildFixture()

      when(fixture.integrationCatalogueConnector.getApis(eqTo(None))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getApis()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }

    "must call the integration catalogue connector and return API details if a platform filter is specified" in {
      val expected = Seq(sampleApiDetailSummary())

      val fixture = buildFixture()
      val platform = "test-platform"

      when(fixture.integrationCatalogueConnector.getApis(eqTo(Some(platform)))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getApis(Some(platform))(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "getUserApis" - {
    val email = "test@hmrc.gov.uk"
    "must call the applications and integration catalogue connectors and return some API details when user has teams" in {
      val expected = Seq(sampleApiDetail())

      val fixture = buildFixture()

      when(fixture.integrationCatalogueConnector.filterApis(eqTo(Seq(email)))(any()))
        .thenReturn(Future.successful(expected))

      when(fixture.applicationsConnector.findTeams(eqTo(Some(email)))(any()))
        .thenReturn(Future.successful(Seq(Team("teamId1", "Team 1", LocalDateTime.now(), Seq.empty))))

      when(fixture.integrationCatalogueConnector.filterApis(eqTo(Seq("teamId1")))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getUserApis(TeamMember(email))(HeaderCarrier()) map {
        actual => {
          verify(fixture.applicationsConnector).findTeams(eqTo(Some(email)))(any())
          verify(fixture.integrationCatalogueConnector).filterApis(eqTo(Seq("teamId1")))(any())
          actual mustBe expected
        }
      }
    }

    "must not call integration catalogue connector and return empty list if user has no teams" in {
      val expected = Seq.empty

      val fixture = buildFixture()

      when(fixture.applicationsConnector.findTeams(eqTo(Some(email)))(any()))
        .thenReturn(Future.successful(Seq.empty))

      fixture.service.getUserApis(TeamMember(email))(HeaderCarrier()) map {
        actual => {
          verify(fixture.applicationsConnector).findTeams(eqTo(Some(email)))(any())
          verifyNoInteractions(fixture.integrationCatalogueConnector)
          actual mustBe expected
        }
      }
    }
  }

  "deepSearchApis" - {
    "must call the integration catalogue connector and return API details" in {
      val expected = Seq(sampleApiDetail())
      val searchTerm = "hello"

      val fixture = buildFixture()

      when(fixture.integrationCatalogueConnector.deepSearchApis(eqTo(searchTerm))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.deepSearchApis(searchTerm)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "addApi" - {
    "must call the applications connector with correct request and return something" in {
      val fixture = buildFixture()

      val applicationId = "applicationId"
      val apiId = "apiId"
      val apiTitle = "apiTitle"
      val verb = "GET"
      val path = "/foo/bar"
      val scopes = Seq("test-scope-1", "test-scope-2")
      val availableEndpoints = Seq(AvailableEndpoint(path, EndpointMethod(verb, None, None, scopes), false))

      val apiRequest = AddApiRequest(apiId, apiTitle, Seq(AddApiRequestEndpoint(verb, path)), scopes)

      val expected = Some(())
      when(fixture.applicationsConnector.addApi(eqTo(applicationId), eqTo(apiRequest))(any())).thenReturn(Future.successful(expected))

      fixture.service.addApi(applicationId, apiId, apiTitle, availableEndpoints)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "removeApi" - {
    "must return the response from the applications connector" in {
      val applicationId = "test-application-id"
      val apiId = "test-api-id"

      val results = Table(
        "response",
        Some(()),
        None
      )

      forAll(results){ response =>
        val fixture = buildFixture()

        when(fixture.applicationsConnector.removeApi(eqTo(applicationId), eqTo(apiId))(any()))
          .thenReturn(Future.successful(response))

        fixture.service.removeApi(applicationId, apiId)(HeaderCarrier()).map {
          result =>
            result mustBe response
        }
      }
    }
  }

  "addCredential" - {
    "must call the applications connector with the correct request and return the response" in {
      val fixture = buildFixture()
      val expected = Credential("test-client-id", LocalDateTime.now(), Some("test-secret"), Some("test-fragment"), FakeHipEnvironments.production.id)

      when(fixture.applicationsConnector.addCredential(eqTo(FakeApplication.id), eqTo(FakeHipEnvironments.production))(any()))
        .thenReturn(Future.successful(Right(Some(expected))))

      fixture.service.addCredential(FakeApplication.id, FakeHipEnvironments.production)(HeaderCarrier()).map {
        actual =>
          actual mustBe Right(Some(expected))
      }
    }
  }

  "deleteCredential" - {
    "must call the applications connector with the correct request and return the response" in {
      val fixture = buildFixture()
      val clientId = "test-client-id"

      when(fixture.applicationsConnector.deleteCredential(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(Some(()))))

      fixture.service.deleteCredential(FakeApplication.id, FakeHipEnvironments.production, clientId)(HeaderCarrier()).map {
        actual =>
          verify(fixture.applicationsConnector).deleteCredential(
            eqTo(FakeApplication.id),
            eqTo(FakeHipEnvironments.production),
            eqTo(clientId))(any()
          )

          actual mustBe Right(Some(()))
      }
    }
  }

  "getAccessRequests" - {
    "must make the correct request to the applications connector and return the access requests" in {
      val fixture = buildFixture()

      val filters = Table(
        ("Application Id", "Status"),
        (Some("test-application-id"), Some(Rejected)),
        (Some("test-application-id"), None),
        (None, Some(Rejected)),
        (None, None)
      )

      forAll(filters) {(applicationIdFilter: Option[String], statusFilter: Option[AccessRequestStatus]) =>
        val expected = sampleAccessRequests()
        when(fixture.applicationsConnector.getAccessRequests(any(), any())(any())).thenReturn(Future.successful(expected))

        fixture.service.getAccessRequests(applicationIdFilter, statusFilter)(HeaderCarrier()).map {
          result =>
            verify(fixture.applicationsConnector).getAccessRequests(eqTo(applicationIdFilter), eqTo(statusFilter))(any())
            result mustBe expected
        }
      }
    }
  }

  "getAccessRequest" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()

      val accessRequest = sampleAccessRequest()

      val accessRequests = Table(
        ("Id", "Access Request"),
        (accessRequest.id, Some(accessRequest)),
        ("test-id", None)
      )

      forAll(accessRequests) {(id: String, accessRequest: Option[AccessRequest]) =>
        when(fixture.applicationsConnector.getAccessRequest(any())(any())).thenReturn(Future.successful(accessRequest))

        fixture.service.getAccessRequest(id)(HeaderCarrier()).map {
          result =>
            verify(fixture.applicationsConnector).getAccessRequest(eqTo(id))(any())
            result mustBe accessRequest
        }
      }
    }
  }

  "approveAccessRequest" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()
      val id = "test-id"
      val decidedBy = "test-decided-by"

      when(fixture.applicationsConnector.approveAccessRequest(any(), any())(any())).thenReturn(Future.successful(Some(())))

      fixture.service.approveAccessRequest(id, decidedBy)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).approveAccessRequest(eqTo(id), eqTo(decidedBy))(any())
          result mustBe Some(())
      }
    }
  }

  "cancelAccessRequest" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()
      val id = "test-id"
      val cancelledBy = "test-cancelled-by"

      when(fixture.applicationsConnector.cancelAccessRequest(any(), any())(any())).thenReturn(Future.successful(Some(())))

      fixture.service.cancelAccessRequest(id, cancelledBy)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).cancelAccessRequest(eqTo(id), eqTo(cancelledBy))(any())
          result mustBe Some(())
      }
    }
  }


  "rejectAccessRequest" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()
      val id = "test-id"
      val decidedBy = "test-decided-by"
      val rejectedReason = "test-rejected-reason"

      when(fixture.applicationsConnector.rejectAccessRequest(any(), any(), any())(any())).thenReturn(Future.successful(Some(())))

      fixture.service.rejectAccessRequest(id, decidedBy, rejectedReason)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).rejectAccessRequest(eqTo(id), eqTo(decidedBy), eqTo(rejectedReason))(any())
          result mustBe Some(())
      }
    }
  }

  "requestProductionAccess" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()

      when(fixture.applicationsConnector.createAccessRequest(any())(any())).thenReturn(Future.successful(()))

      val anAccessRequest = AccessRequestRequest("appId", "blah", "me@here", Seq.empty, "test")

      fixture.service.requestProductionAccess(anAccessRequest)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).createAccessRequest(eqTo(anAccessRequest))(any())
          result mustBe ()
      }
    }
  }

  "addTeamMember" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()

      val applicationId = "test-id"
      val teamMember = TeamMember("test-email")

      when(fixture.applicationsConnector.addTeamMember(any(), any())(any())).thenReturn(Future.successful(Some(())))

      fixture.service.addTeamMember(applicationId, teamMember)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).addTeamMember(eqTo(applicationId), eqTo(teamMember))(any())
          result.value mustBe ()
      }
    }
  }

  "findTeamById" - {
    "must return the team from the applications connector when it exists" in {
      val fixture = buildFixture()

      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember("test-email")))

      when(fixture.service.findTeamById(any())(any())).thenReturn(Future.successful(Some(team)))

      fixture.service.findTeamById(team.id)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).findTeamById(eqTo(team.id))(any())
          result mustBe Some(team)
      }
    }

    "must return None when the team does not exist" in {
      val fixture = buildFixture()

      val id = "test-team-id"

      when(fixture.service.findTeamById(any())(any())).thenReturn(Future.successful(None))

      fixture.service.findTeamById(id)(HeaderCarrier()).map {
        result =>
          result mustBe None
      }
    }
  }

  "findTeamByName" - {
    "must return the team from the applications connector when it exists" in {
      val fixture = buildFixture()

      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember("test-email")))

      when(fixture.service.findTeamByName(any())(any())).thenReturn(Future.successful(Some(team)))

      fixture.service.findTeamByName(team.name)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).findTeamByName(eqTo(team.name))(any())
          result mustBe Some(team)
      }
    }

    "must return None when the team does not exist" in {
      val fixture = buildFixture()

      val name = "test-team-name"

      when(fixture.service.findTeamByName(any())(any())).thenReturn(Future.successful(None))

      fixture.service.findTeamByName(name)(HeaderCarrier()).map {
        result =>
          result mustBe None
      }
    }
  }

  "findTeams" - {
    "must return the teams from the applications connector" in {
      val fixture = buildFixture()
      val teamMemberEmail = "test-email"

      val teams = Seq(Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember("test-email"))))

      when(fixture.service.findTeams(eqTo(Some(teamMemberEmail)))(any())).thenReturn(Future.successful(teams))

      fixture.service.findTeams(Some(teamMemberEmail))(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).findTeams(eqTo(Some(teamMemberEmail)))(any())
          result mustBe teams
      }
    }
  }

  "createTeam" - {
    "must make the correct request to the applications connector and return the created team" in {
      val fixture = buildFixture()

      val newTeam = NewTeam("test-team-name", Seq(TeamMember("test-email")))
      val team = Team("test-team-id", newTeam.name, LocalDateTime.now(), newTeam.teamMembers)

      when(fixture.applicationsConnector.createTeam(any())(any())).thenReturn(Future.successful(Right(team)))

      fixture.service.createTeam(newTeam)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).createTeam(eqTo(newTeam))(any())
          result.value mustBe team
      }
    }
  }

  "addTeamMemberToTeam" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()

      val teamId = "test-id"
      val teamMember = TeamMember("test-email")

      when(fixture.applicationsConnector.addTeamMemberToTeam(any(), any())(any())).thenReturn(Future.successful(Some(())))

      fixture.service.addTeamMemberToTeam(teamId, teamMember)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).addTeamMemberToTeam(eqTo(teamId), eqTo(teamMember))(any())
          result.value mustBe ()
      }
    }
  }

  "removeTeamMemberFromTeam" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()

      val teamId = "test-id"
      val teamMember = TeamMember("test-email")

      when(fixture.applicationsConnector.removeTeamMemberFromTeam(any(), any())(any())).thenReturn(Future.successful(Some(())))

      fixture.service.removeTeamMemberFromTeam(teamId, teamMember)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).removeTeamMemberFromTeam(eqTo(teamId), eqTo(teamMember))(any())
          result.value mustBe ()
      }
    }
  }

  "getUserContactDetails" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()
      val users = Seq(
        UserContactDetails("user1@example.com"),
        UserContactDetails("user2@example.com")
      )

      when(fixture.applicationsConnector.getUserContactDetails()(any())).thenReturn(Future.successful(users))

      fixture.service.getUserContactDetails()(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).getUserContactDetails()(any())
          result mustBe users
      }
    }
  }

  "updateApplicationTeam" - {
    "must make the correct update request to the applications connector and return successfully" in {
      val fixture = buildFixture()
      val teamId = "test-team-id"
      val applicationId = "test-app-id"

      when(fixture.applicationsConnector.updateApplicationTeam(eqTo(applicationId), eqTo(teamId))(any())).thenReturn(Future.successful(Some(())))

      fixture.service.updateApplicationTeam(applicationId, Some(teamId))(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).updateApplicationTeam(eqTo(applicationId), eqTo(teamId))(any())
          result mustBe Some(())
      }
    }

    "must make the correct delete request to the applications connector and return successfully" in {
      val fixture = buildFixture()
      val applicationId = "test-app-id"

      when(fixture.applicationsConnector.removeApplicationTeam(eqTo(applicationId))(any())).thenReturn(Future.successful(Some(())))

      fixture.service.updateApplicationTeam(applicationId, None)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).removeApplicationTeam(eqTo(applicationId))(any())
          result mustBe Some(())
      }
    }
  }

  "updateApiTeam" - {
    "must make the correct update request to the applications connector and return successfully" in {
      val fixture = buildFixture()
      val teamId = "test-team-id"
      val apiId = "test-api-id"

      when(fixture.applicationsConnector.updateApiTeam(eqTo(apiId), eqTo(teamId))(any())).thenReturn(Future.successful(Some(())))

      fixture.service.updateApiTeam(apiId, Some(teamId))(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).updateApiTeam(eqTo(apiId), eqTo(teamId))(any())
          result mustBe Some(())
      }
    }

    "must make the correct delete request to the applications connector and return successfully" in {
      val fixture = buildFixture()
      val apiId = "test-api-id"

      when(fixture.applicationsConnector.removeApiTeam(eqTo(apiId))(any())).thenReturn(Future.successful(Some(())))

      fixture.service.updateApiTeam(apiId, None)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).removeApiTeam(eqTo(apiId))(any())
          result mustBe Some(())
      }
    }
  }
  
  "getPlatformContact" - {

    "must make the correct request to the integration catalogue connector and return successfully" in {
      val fixture = buildFixture()
      val expected = Seq(
        PlatformContact("A_PLATFORM", ContactInfo("a name", "an email"), false),
        PlatformContact("ANOTHER_PLATFORM", ContactInfo("another name", "another email"), false)
      )
      when(fixture.integrationCatalogueConnector.getPlatformContacts()(any())).thenReturn(Future.successful(expected))

      fixture.service.getPlatformContact("A_PLATFORM")(HeaderCarrier(), executionContext).map {
        result =>
          verify(fixture.integrationCatalogueConnector).getPlatformContacts()(any())
          result mustBe Some(PlatformContact("A_PLATFORM", ContactInfo("a name", "an email"), false))
      }
    }
  }

  "apisInProduction" - {
    "must make the correct request to the applications connector and return the statistic" in {
      val fixture = buildFixture()
      val expected = ApisInProductionStatistic(10, 2)

      when(fixture.applicationsConnector.apisInProduction()(any)).thenReturn(Future.successful(expected))

      fixture.service.apisInProduction()(HeaderCarrier()).map {
        result =>
          result mustBe expected
      }
    }
  }

  "listApisInProduction" - {
    "must make the correct request to the applications connector and return the api details" in {
      val fixture = buildFixture()
      val apis = Seq(sampleApiDetailSummary(), sampleApiDetailSummary())

      when(fixture.applicationsConnector.listApisInProduction()(any)).thenReturn(Future.successful(apis))

      fixture.service.listApisInProduction()(HeaderCarrier()).map {
        result =>
          result mustBe apis
      }
    }
  }

  "listEgressGateways" - {
    "must make the correct request to the applications connector and return the egress gateways" in {
      val fixture = buildFixture()
      val environment = FakeHipEnvironments.production

      val egressGateways = sampleEgressGateways()
      when(fixture.applicationsConnector.listEgressGateways(eqTo(environment))(any)).thenReturn(Future.successful(egressGateways))

      fixture.service.listEgressGateways(environment)(HeaderCarrier()).map {
        result =>
          result mustBe egressGateways
      }
    }
  }

  "generateDeployment" - {
    "must make the correct request to the applications connector and return the deployment response" in {
      val fixture = buildFixture()

      val name = "test-name"
      val deploymentsRequest: DeploymentsRequest = DeploymentsRequest(
        lineOfBusiness = "test-line-of-business",
        name = name,
        description = "test-description",
        egress = Some("test-egress"),
        teamId = "test-team-id",
        oas = "test-oas",
        passthrough = false,
        status = "test-status",
        domain = "test-domain",
        subDomain = "test-sub-domain",
        hods = Seq("hod1"),
        prefixesToRemove = Seq.empty,
        egressMappings = None,
        basePath = "test-base-path"
      )
      val response = SuccessfulDeploymentsResponse("id", "version", 1, "uri.com")

      when(fixture.applicationsConnector.generateDeployment(any)(any))
        .thenReturn(Future.successful(response))

      fixture.service.generateDeployment(deploymentsRequest)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).generateDeployment(eqTo(deploymentsRequest))(any())
          result mustBe response
      }
    }
  }

  "fetchAllScopes" - {
    "must make the correct request to the applications connector and return the scopes" in {
      val fixture = buildFixture()
      val applicationId = "test-application-id"
      val credentialScopes = (1 to 2).map(
        i =>
          CredentialScopes(
            environmentId = FakeHipEnvironments.production.id,
            clientId = s"test-client-id-$i",
            created = LocalDateTime.now(),
            scopes = Seq(s"test-scope-$i-1", s"test-scope-$i-2")
          )
      )

      when(fixture.applicationsConnector.fetchAllScopes(eqTo(applicationId))(any))
        .thenReturn(Future.successful(Some(credentialScopes)))

      fixture.service.fetchAllScopes(applicationId)(HeaderCarrier()).map {
        result =>
          result.value mustBe credentialScopes
      }
    }
  }

  "fetchCredentials" - {
    "must make the correct request to the applications connector and return the expected credentials" in {
      val fixture = buildFixture()
      val applicationId = "test-application-id"
      val environment = FakeHipEnvironments.test
      val credentials = (1 to 2).map(
        i =>
          Credential(
            clientId = s"test-client-id-$i",
            created = LocalDateTime.now(),
            clientSecret = None,
            secretFragment = None,
            environmentId = environment.id
          )
      )

      when(fixture.applicationsConnector.fetchCredentials(eqTo(applicationId), eqTo(environment))(any))
        .thenReturn(Future.successful(Some(credentials)))

      fixture.service.fetchCredentials(applicationId, environment)(HeaderCarrier()).map {
        result =>
          result.value mustBe credentials
      }
    }
  }

  "fixScopes" - {
    "must make the correct request to the applications connector and return the result" in {
      val fixture = buildFixture()
      val applicationId = "test-application-id"

      when(fixture.applicationsConnector.fixScopes(eqTo(applicationId))(any))
        .thenReturn(Future.successful(Some(())))

      fixture.service.fixScopes(applicationId)(HeaderCarrier()).map {
        result =>
          result.value mustBe ()
      }
    }
  }

  "forcePublish" - {
    "must make the correct request to the applications connector and return the result" in {
      val fixture = buildFixture()
      val publisherReference = "test-publisher-reference"

      when(fixture.applicationsConnector.forcePublish(any)(any))
        .thenReturn(Future.successful(Some(())))

      fixture.service.forcePublish(publisherReference)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).forcePublish(eqTo(publisherReference))(any)
          result.value mustBe ()
      }
    }
  }

  "listEnvironments" - {
    "must make the correct request to the applications connector and return the environments" in {
      val fixture = buildFixture()

      val productionBaseEnv = BaseHipEnvironment(
        id = "production",
        rank = 1,
        isProductionLike = true,
        promoteTo = None
      )

      val testBaseEnv = BaseHipEnvironment(
        id = "test",
        rank = 2,
        isProductionLike = false,
        promoteTo = Some("production")
      )

      val shareableConfig = ShareableHipConfig(Seq(productionBaseEnv, testBaseEnv), "production", "test")

      when(fixture.applicationsConnector.listEnvironments()(any)).thenReturn(Future.successful(shareableConfig))

      fixture.service.listEnvironments()(HeaderCarrier()).map {
        result =>
          result mustBe shareableConfig
      }
    }
  }

  "testApimEndpoint" - {
    "must make the correct APIM request and return the result" in {
      val fixture = buildFixture()
      val env = FakeHipEnvironments.production
      val apimRequest = mock[ApimRequest[?]]
      val params = Seq("p1", "p2")
      val apimResponse = "hello"

      when(apimRequest.makeRequest(eqTo(fixture.apimConnector), eqTo(env), eqTo(params))(any(), any()))
        .thenReturn(Future.successful(Right(apimResponse)))

      fixture.service.testApimEndpoint(env, apimRequest, params)(HeaderCarrier()).map {
        result =>
          result mustBe Right(apimResponse)
      }
    }
  }

  "fetchDashboardStatistics" - {
    "must fetch the data, pass to the builder, and return the result" in {
      val fixture = buildFixture()

      val report = Seq(
        IntegrationPlatformReport(
          platformType = "HIP",
          integrationType = ApiDetail.IntegrationType.api,
          count = 12
        ),
        IntegrationPlatformReport(
          platformType = "NOT-HIP",
          integrationType = ApiDetail.IntegrationType.api,
          count = 34
        )
      )

      val expected = DashboardStatistics(totalApis = 12 + 34, selfServiceApis = 12)

      when(fixture.integrationCatalogueConnector.getReport()(any))
        .thenReturn(Future.successful(report))

      fixture.service.fetchDashboardStatistics()(HeaderCarrier()).map {
        result =>
          result mustBe expected
      }
    }

    "addEgressesToTeam" - {
      "must make the correct request to the applications connector" in {
        val fixture = buildFixture()

        when(fixture.applicationsConnector.addEgressesToTeam(any, any)(any)).thenReturn(Future.successful(Some(())))

        fixture.service.addEgressesToTeam("team-id", Set("egress1"))(HeaderCarrier()).map {
          result =>
            verify(fixture.applicationsConnector).addEgressesToTeam(eqTo("team-id"), eqTo(Set("egress1")))(any)
            result mustBe Some(())
        }
      }
    }
  }

  "removeEgressFromTeam" - {
    "must make the correct request to the connector and return the response" in {
      val teamId = "test-team-id"
      val egressId = "test-egress-id"

      val scenarios = Table(
        "expected",
        Some(()),
        None
      )

      forAll(scenarios) {expected =>
        val fixture = buildFixture()

        when(fixture.applicationsConnector.removeEgressFromTeam(any, any)(any)).thenReturn(Future.successful(expected))

        fixture.service.removeEgressFromTeam(teamId, egressId)(HeaderCarrier()).map {
          result =>
            verify(fixture.applicationsConnector).removeEgressFromTeam(eqTo(teamId), eqTo(egressId))(any)
            result mustBe expected
        }
      }
    }
  }

  private case class Fixture(
    applicationsConnector: ApplicationsConnector,
    integrationCatalogueConnector: IntegrationCatalogueConnector,
    apimConnector: ApimConnector,
    service: ApiHubService
  )

  private def buildFixture(): Fixture = {
    val applicationsConnector = mock[ApplicationsConnector]
    val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
    val apimConnector = mock[ApimConnector]
    val dashboardStatisticsBuilder = new DashboardStatisticsBuilder(FakePlatforms)
    val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector, apimConnector, dashboardStatisticsBuilder, FakePlatforms)

    Fixture(applicationsConnector, integrationCatalogueConnector, apimConnector, service)
  }

}

trait ApplicationGetterBehaviours extends AsyncFreeSpec with Matchers with MockitoSugar {

  def successfulApplicationGetter(includeDeleted: Boolean): Unit = {
    s"must call the applications connector and return an application" in {
      val application = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val expected = Some(application)

      val applicationsConnector = mock[ApplicationsConnector]
      val apimConnector = mock[ApimConnector]
      when(applicationsConnector.getApplication(eqTo("id-1"), eqTo(includeDeleted))(any())).thenReturn(Future.successful(expected))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val dashboardStatisticsBuilder = new DashboardStatisticsBuilder(FakePlatforms)
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector, apimConnector, dashboardStatisticsBuilder, FakePlatforms)

      service.getApplication("id-1")(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getApplication(eqTo("id-1"), eqTo(includeDeleted))(any())
          succeed
      }
    }
  }

}
