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
import models.accessrequest.{AccessRequest, AccessRequestStatus, Rejected}
import models.api.EndpointMethod
import models.application._
import models.requests.{AddApiRequest, AddApiRequestEndpoint}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.OptionValues
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
    with ApplicationGetterBehaviours
    with ApiDetailGenerators
    with AccessRequestGenerator
    with TableDrivenPropertyChecks {

  "registerApplication" - {
    "must call the applications connector and return the saved application" in {
      val newApplication = NewApplication("test-app-name", Creator("test-creator-email"), Seq(TeamMember("test-creator-email")))
      val expected = Application("id", newApplication)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.registerApplication(ArgumentMatchers.eq(newApplication))(any()))
        .thenReturn(Future.successful(expected))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.registerApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).registerApplication(ArgumentMatchers.eq(newApplication))(any())
          succeed
      }
    }
  }

  "getApplications" - {
    "must call the applications connector and return a sequence of applications" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.getApplications()(any())).thenReturn(Future.successful(expected))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.getApplications()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getApplications()(any())
          succeed
      }
    }

    "must" - {
      behave like successfulUserApplicationsGetter(enrich = true)
    }

    "must" - {
      behave like successfulUserApplicationsGetter(enrich = false)
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
      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.deleteApplication(ArgumentMatchers.eq(id), ArgumentMatchers.eq(userEmail))(any()))
        .thenReturn(Future.successful(Some(())))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.deleteApplication(id, userEmail)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
          verify(applicationsConnector).deleteApplication(any(), ArgumentMatchers.eq(userEmail))(any())
          succeed
      }
    }

    "must return None when the applications connectors does to indicate the application was not found" in {
      val id = "test-id"

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.deleteApplication(ArgumentMatchers.eq(id), any())(any()))
        .thenReturn(Future.successful(None))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.deleteApplication(id, None)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "requestAdditionalScope" - {
    "must call the applications connector and return the new scope" in {
      val applicationId = "app-id"
      val newScope = NewScope(applicationId, Seq(Primary))

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.requestAdditionalScope(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(newScope))(any()))
        .thenReturn(Future.successful(Some(newScope)))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.requestAdditionalScope(applicationId, newScope)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(newScope)
          verify(applicationsConnector).requestAdditionalScope(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(newScope))(any())
          succeed
      }
    }
  }

  "addScopes" - {
    "must call the applications connector with the correct request" in {
      val applicationId = "app-id"
      val scopes = Set("test-scope-1", "test-scope-2")
      val newScopes = scopes.map(NewScope(_, Seq(Secondary))).toSeq

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.addScopes(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(newScopes))(any()))
        .thenReturn(Future.successful(Some(())))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.addScopes(applicationId, scopes)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
      }
    }
  }

  "pendingScopes" - {
    "must call the applications connectors and return the applications" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.pendingPrimaryScopes()(any())).thenReturn(Future.successful(expected))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.pendingPrimaryScopes()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).pendingPrimaryScopes()(any())
          succeed
      }
    }
  }

  "approveScope" - {
    "must call the applications connectors and return APPROVED" in {
      val appId = "app_id"
      val scope = "a_scope"
      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.approvePrimaryScope(ArgumentMatchers.eq(appId),ArgumentMatchers.eq(scope))(any()))
        .thenReturn(Future.successful(true))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.approvePrimaryScope(appId,scope)(HeaderCarrier()) map {
        actual =>
          actual mustBe true
          verify(applicationsConnector).approvePrimaryScope(ArgumentMatchers.eq(appId),ArgumentMatchers.eq(scope))(any())
          succeed
      }
    }
  }

  "createPrimarySecret" - {
    "must call the applications connector and return the secret" in {
      val applicationId = "test-application-id"
      val expected = Secret("test-secret")
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      when(applicationsConnector.createPrimarySecret(ArgumentMatchers.eq(applicationId))(any()))
        .thenReturn(Future.successful(Some(expected)))

      service.createPrimarySecret(applicationId)(HeaderCarrier()) map {
        actual =>
          actual.value mustBe expected
      }
    }
  }

  "testConnectivity" - {
    "must call the applications connector and return something" in {
      val expected = "something"
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      when(applicationsConnector.testConnectivity()(any()))
        .thenReturn(Future.successful(expected))

      service.testConnectivity()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "getApiDetail" - {
    "must call the integration catalogue connector and return the API detail" in {
      val expected = sampleApiDetail()

      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      when(integrationCatalogueConnector.getApiDetail(ArgumentMatchers.eq(expected.id))(any()))
        .thenReturn(Future.successful(Some(expected)))

      service.getApiDetail(expected.id)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }
  }

  "getAllApis" - {
    "must call the integration catalogue connector and some API details" in {
      val expected = Seq(sampleApiDetail())

      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      when(integrationCatalogueConnector.getAllHipApis()(any()))
        .thenReturn(Future.successful(expected))

      service.getAllHipApis()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "addApi" - {
    "must call the applications connector with correct request and return something" in {
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      val applicationId = "applicationId"
      val apiId = "apiId"
      val verb = "GET"
      val path = "/foo/bar"
      val scopes = Seq("test-scope-1", "test-scope-2")
      val availableEndpoints = Seq(AvailableEndpoint(path, EndpointMethod(verb, None, None, scopes)))

      val apiRequest = AddApiRequest(apiId, Seq(AddApiRequestEndpoint(verb, path)), scopes)

      val expected = Some(())
      when(applicationsConnector.addApi(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(apiRequest))(any())).thenReturn(Future.successful(expected))

      service.addApi(applicationId, apiId, availableEndpoints)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "addCredential" - {
    "must call the applications connector with the correct request and return the response" in {
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)
      val expected = Credential("test-client-id", LocalDateTime.now(), Some("test-secret"), Some("test-fragment"))

      when(applicationsConnector.addCredential(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(Primary))(any()))
        .thenReturn(Future.successful(Right(Some(expected))))

      service.addCredential(FakeApplication.id, Primary)(HeaderCarrier()).map {
        actual =>
          actual mustBe Right(Some(expected))
      }
    }
  }

  "getAccessRequests" - {
    "must make the correct request to the applications connector and return the access requests" in {
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      val filters = Table(
        ("Application Id", "Status"),
        (Some("test-application-id"), Some(Rejected)),
        (Some("test-application-id"), None),
        (None, Some(Rejected)),
        (None, None)
      )

      forAll(filters) {(applicationIdFilter: Option[String], statusFilter: Option[AccessRequestStatus]) =>
        val expected = sampleAccessRequests()
        when(applicationsConnector.getAccessRequests(any(), any())(any())).thenReturn(Future.successful(expected))

        service.getAccessRequests(applicationIdFilter, statusFilter)(HeaderCarrier()).map {
          result =>
            verify(applicationsConnector).getAccessRequests(ArgumentMatchers.eq(applicationIdFilter), ArgumentMatchers.eq(statusFilter))(any())
            result mustBe expected
        }
      }
    }
  }

  "getAccessRequest" - {
    "must make the correct request to the applications connector and return the response" in {
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      val accessRequest = sampleAccessRequest()

      val accessRequests = Table(
        ("Id", "Access Request"),
        (accessRequest.id, Some(accessRequest)),
        ("test-id", None)
      )

      forAll(accessRequests) {(id: String, accessRequest: Option[AccessRequest]) =>
        when(applicationsConnector.getAccessRequest(any())(any())).thenReturn(Future.successful(accessRequest))

        service.getAccessRequest(id)(HeaderCarrier()).map {
          result =>
            verify(applicationsConnector).getAccessRequest(ArgumentMatchers.eq(id))(any())
            result mustBe accessRequest
        }
      }
    }
  }

  "approveAccessRequest" - {
    "must make the correct request to the applications connector and return the response" in {
      val applicationsConnector = mock[ApplicationsConnector]
      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)
      val id = "test-id"
      val decidedBy = "test-decided-by"

      when(applicationsConnector.approveAccessRequest(any(), any())(any())).thenReturn(Future.successful(Some(())))

      service.approveAccessRequest(id, decidedBy)(HeaderCarrier()).map {
        result =>
          verify(applicationsConnector).approveAccessRequest(ArgumentMatchers.eq(id), ArgumentMatchers.eq(decidedBy))(any())
          result mustBe Some(())
      }
    }
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

  def successfulUserApplicationsGetter(enrich: Boolean): Unit = {
    s"must call the applications connector and return a user's applications when enrich is $enrich" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.getUserApplications(ArgumentMatchers.eq("test-creator-email-2"), ArgumentMatchers.eq(enrich))(any()))
        .thenReturn(Future.successful(expected))

      val integrationCatalogueConnector = mock[IntegrationCatalogueConnector]
      val service = new ApiHubService(applicationsConnector, integrationCatalogueConnector)

      service.getUserApplications("test-creator-email-2", enrich = enrich)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getUserApplications(ArgumentMatchers.eq("test-creator-email-2"), ArgumentMatchers.eq(enrich))(any())
          succeed
      }
    }
  }

}
