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

import connectors.{ApplicationsConnector, IntegrationCatalogueConnector}
import controllers.actions.FakeApplication
import generators.{AccessRequestGenerator, ApiDetailGenerators}
import models.AvailableEndpoint
import models.accessrequest._
import models.api.{ApiDeploymentStatuses, EndpointMethod}
import models.application.{Application, Creator, Credential, Deleted, NewApplication, Primary, TeamMember}
import models.application.ApplicationLenses._
import models.requests.{AddApiRequest, AddApiRequestEndpoint}
import models.team.{NewTeam, Team}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Future

class ApiHubServiceSpec
  extends AsyncFreeSpec
    with Matchers
    with MockitoSugar
    with OptionValues
    with EitherValues
    with ApplicationGetterBehaviours
    with ApiDetailGenerators
    with AccessRequestGenerator
    with TableDrivenPropertyChecks {

  "registerApplication" - {
    "must call the applications connector and return the saved application" in {
      val newApplication = NewApplication("test-app-name", Creator("test-creator-email"), Seq(TeamMember("test-creator-email")))
      val expected = Application("id", newApplication)

      val fixture = buildFixture()
      when(fixture.applicationsConnector.registerApplication(ArgumentMatchers.eq(newApplication))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.registerApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).registerApplication(ArgumentMatchers.eq(newApplication))(any())
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
          verify(fixture.applicationsConnector).getApplications(ArgumentMatchers.eq(None), ArgumentMatchers.eq(false))(any())
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
          verify(fixture.applicationsConnector).getApplications(ArgumentMatchers.eq(None), ArgumentMatchers.eq(true))(any())
          succeed
      }
    }

    "must call the applications connector and return a user's applications when requested" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val fixture = buildFixture()

      when(fixture.applicationsConnector.getApplications(ArgumentMatchers.eq(Some("test-creator-email-2")), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getApplications(Some("test-creator-email-2"), false)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(fixture.applicationsConnector).getApplications(ArgumentMatchers.eq(Some("test-creator-email-2")), ArgumentMatchers.eq(false))(any())
          succeed
      }
    }
  }

  "getApplication" - {
    "must" - {
      behave like successfulApplicationGetter(true)
    }

    "must" - {
      behave like successfulApplicationGetter(false)
    }
  }

  "deleteApplication" - {
    "must delete the application via the applications connector for the specified user" in {
      val id = "test-id"

      val userEmail = Some("me@test.com")
      val fixture = buildFixture()
      when(fixture.applicationsConnector.deleteApplication(ArgumentMatchers.eq(id), ArgumentMatchers.eq(userEmail))(any()))
        .thenReturn(Future.successful(Some(())))

      fixture.service.deleteApplication(id, userEmail)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
          verify(fixture.applicationsConnector).deleteApplication(any(), ArgumentMatchers.eq(userEmail))(any())
          succeed
      }
    }

    "must return None when the applications connectors does to indicate the application was not found" in {
      val id = "test-id"

      val fixture = buildFixture()
      when(fixture.applicationsConnector.deleteApplication(ArgumentMatchers.eq(id), any())(any()))
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

      when(fixture.integrationCatalogueConnector.getApiDetail(ArgumentMatchers.eq(expected.id))(any()))
        .thenReturn(Future.successful(Some(expected)))

      fixture.service.getApiDetail(expected.id)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }
  }

  "getApiDeploymentStatuses" - {
    "must call the applications connector and return the API detail" in {
      val publisherReference = "ref123"
      val expected = ApiDeploymentStatuses(true, false)

      val fixture = buildFixture()

      when(fixture.applicationsConnector.getApiDeploymentStatuses(ArgumentMatchers.eq(publisherReference))(any()))
        .thenReturn(Future.successful(Some(expected)))

      fixture.service.getApiDeploymentStatuses(publisherReference)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }
  }

  "getAllApis" - {
    "must call the integration catalogue connector and some API details" in {
      val expected = Seq(sampleApiDetail())

      val fixture = buildFixture()

      when(fixture.integrationCatalogueConnector.getAllHipApis()(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getAllHipApis()(HeaderCarrier()) map {
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

      when(fixture.integrationCatalogueConnector.filterApis(ArgumentMatchers.eq(Seq(email)))(any()))
        .thenReturn(Future.successful(expected))

      when(fixture.applicationsConnector.findTeams(ArgumentMatchers.eq(Some(email)))(any()))
        .thenReturn(Future.successful(Seq(Team("teamId1", "Team 1", LocalDateTime.now(), Seq.empty))))

      when(fixture.integrationCatalogueConnector.filterApis(ArgumentMatchers.eq(Seq("teamId1")))(any()))
        .thenReturn(Future.successful(expected))

      fixture.service.getUserApis(TeamMember(email))(HeaderCarrier(), executionContext) map {
        actual => {
          verify(fixture.applicationsConnector).findTeams(ArgumentMatchers.eq(Some(email)))(any())
          verify(fixture.integrationCatalogueConnector).filterApis(ArgumentMatchers.eq(Seq("teamId1")))(any())
          actual mustBe expected
        }
      }
    }

    "must not call integration catalogue connector and return empty list if user has no teams" in {
      val expected = Seq.empty

      val fixture = buildFixture()

      when(fixture.applicationsConnector.findTeams(ArgumentMatchers.eq(Some(email)))(any()))
        .thenReturn(Future.successful(Seq.empty))

      fixture.service.getUserApis(TeamMember(email))(HeaderCarrier(), executionContext) map {
        actual => {
          verify(fixture.applicationsConnector).findTeams(ArgumentMatchers.eq(Some(email)))(any())
          verifyZeroInteractions(fixture.integrationCatalogueConnector)
          actual mustBe expected
        }
      }
    }
  }

  "addApi" - {
    "must call the applications connector with correct request and return something" in {
      val fixture = buildFixture()

      val applicationId = "applicationId"
      val apiId = "apiId"
      val verb = "GET"
      val path = "/foo/bar"
      val scopes = Seq("test-scope-1", "test-scope-2")
      val availableEndpoints = Seq(AvailableEndpoint(path, EndpointMethod(verb, None, None, scopes), false))

      val apiRequest = AddApiRequest(apiId, Seq(AddApiRequestEndpoint(verb, path)), scopes)

      val expected = Some(())
      when(fixture.applicationsConnector.addApi(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(apiRequest))(any())).thenReturn(Future.successful(expected))

      fixture.service.addApi(applicationId, apiId, availableEndpoints)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "addCredential" - {
    "must call the applications connector with the correct request and return the response" in {
      val fixture = buildFixture()
      val expected = Credential("test-client-id", LocalDateTime.now(), Some("test-secret"), Some("test-fragment"))

      when(fixture.applicationsConnector.addCredential(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(Primary))(any()))
        .thenReturn(Future.successful(Right(Some(expected))))

      fixture.service.addCredential(FakeApplication.id, Primary)(HeaderCarrier()).map {
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

      fixture.service.deleteCredential(FakeApplication.id, Primary, clientId)(HeaderCarrier()).map {
        actual =>
          verify(fixture.applicationsConnector).deleteCredential(
            ArgumentMatchers.eq(FakeApplication.id),
            ArgumentMatchers.eq(Primary),
            ArgumentMatchers.eq(clientId))(any()
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
            verify(fixture.applicationsConnector).getAccessRequests(ArgumentMatchers.eq(applicationIdFilter), ArgumentMatchers.eq(statusFilter))(any())
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
            verify(fixture.applicationsConnector).getAccessRequest(ArgumentMatchers.eq(id))(any())
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
          verify(fixture.applicationsConnector).approveAccessRequest(ArgumentMatchers.eq(id), ArgumentMatchers.eq(decidedBy))(any())
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
          verify(fixture.applicationsConnector).rejectAccessRequest(ArgumentMatchers.eq(id), ArgumentMatchers.eq(decidedBy), ArgumentMatchers.eq(rejectedReason))(any())
          result mustBe Some(())
      }
    }
  }

  "requestProductionAccess" - {
    "must make the correct request to the applications connector and return the response" in {
      val fixture = buildFixture()

      when(fixture.applicationsConnector.createAccessRequest(any())(any())).thenReturn(Future.successful(Some(())))

      val anAccessRequest = AccessRequestRequest("appId", "blah", "me@here", Seq.empty)

      fixture.service.requestProductionAccess(anAccessRequest)(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).createAccessRequest(ArgumentMatchers.eq(anAccessRequest))(any())
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
          verify(fixture.applicationsConnector).addTeamMember(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(teamMember))(any())
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
          verify(fixture.applicationsConnector).findTeamById(ArgumentMatchers.eq(team.id))(any())
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
          verify(fixture.applicationsConnector).findTeamByName(ArgumentMatchers.eq(team.name))(any())
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

      when(fixture.service.findTeams(ArgumentMatchers.eq(Some(teamMemberEmail)))(any())).thenReturn(Future.successful(teams))

      fixture.service.findTeams(Some(teamMemberEmail))(HeaderCarrier()).map {
        result =>
          verify(fixture.applicationsConnector).findTeams(ArgumentMatchers.eq(Some(teamMemberEmail)))(any())
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
          verify(fixture.applicationsConnector).createTeam(ArgumentMatchers.eq(newTeam))(any())
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
          verify(fixture.applicationsConnector).addTeamMemberToTeam(ArgumentMatchers.eq(teamId), ArgumentMatchers.eq(teamMember))(any())
          result.value mustBe ()
      }
    }
  }

  private case class Fixture(
    applicationsConnector: ApplicationsConnector,
    integrationCatalogueConnector: IntegrationCatalogueConnector,
    service: ApiHubService
  )

  private def buildFixture(): Fixture = {
    val applicationsConnector = mock[ApplicationsConnector]
    val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
    val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

    Fixture(applicationsConnector, integrationCatalogueConnector, service)
  }

}

trait ApplicationGetterBehaviours {
  this: AsyncFreeSpec with Matchers with MockitoSugar =>

  def successfulApplicationGetter(enrich: Boolean): Unit = {
    s"must call the applications connector with enrich set to $enrich and return an application" in {
      val application = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val expected = Some(application)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.getApplication(ArgumentMatchers.eq("id-1"), ArgumentMatchers.eq(enrich))(any())).thenReturn(Future.successful(expected))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.getApplication("id-1", enrich)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getApplication(ArgumentMatchers.eq("id-1"), ArgumentMatchers.eq(enrich))(any())
          succeed
      }
    }
  }

}
