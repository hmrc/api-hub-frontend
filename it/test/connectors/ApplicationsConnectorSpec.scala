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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import config.FrontendAppConfig
import connectors.ApplicationsConnectorSpec.ApplicationGetterBehaviours
import models.UserEmail
import models.accessrequest._
import models.api.ApiDeploymentStatuses
import models.application._
import models.application.ApplicationLenses._
import models.deployment.{DeploymentsRequest, Error, FailuresResponse, InvalidOasResponse, RedeploymentRequest, SuccessfulDeploymentsResponse}
import models.exception.{ApplicationCredentialLimitException, TeamNameNotUniqueException}
import models.requests.{AddApiRequest, AddApiRequestEndpoint, TeamMemberRequest}
import models.team.{NewTeam, Team}
import models.user.{LdapUser, UserModel}
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
import play.api.http.ContentTypes
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class ApplicationsConnectorSpec
  extends AsyncFreeSpec
  with Matchers
  with WireMockSupport
  with OptionValues
  with EitherValues
  with ApplicationGetterBehaviours
  with TableDrivenPropertyChecks {

  import ApplicationsConnectorSpec._

  "ApplicationsConnector.registerApplication" - {
    "must place the correct request and return the stored application" in {
      val newApplication = NewApplication("test-name", Creator("test-creator-email"), Seq(TeamMember("test-creator-email")))
      val expected = Application("test-id", newApplication)

      stubFor(
        post(urlEqualTo("/api-hub-applications/applications"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .withRequestBody(
            equalToJson(toJsonString(newApplication))
          )
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).registerApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "ApplicationsConnector.getApplications" - {
    "must place the correct request and return the array of applications" in {
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications?includeDeleted=false"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).getApplications(None, false)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }

    "must place the correct request and return the applications including deleted ones when requested" in {
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
        .delete(Deleted(LocalDateTime.now(), "test-deleted-by"))
      val expected = Seq(application1, application2)

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications?includeDeleted=true"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).getApplications(None, true)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }

    "must place the correct request and return the applications for a given user when requested" in {
      val testEmail = "test-user-email-2"
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
        .copy(teamMembers = Seq(TeamMember("test-creator-email-1"), TeamMember(testEmail)))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
        .copy(teamMembers = Seq(TeamMember(testEmail), TeamMember("test-user-email-3")))
      val expected = Seq(application1, application2)
      val crypto = new ApplicationCrypto(ConfigFactory.parseResources("application.conf"))

      val userEmailEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(testEmail)).value
      val userEmailEncoded = URLEncoder.encode(userEmailEncrypted, "UTF-8")
      stubFor(
        get(urlEqualTo(f"/api-hub-applications/applications?teamMember=$userEmailEncoded&includeDeleted=false"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).getApplications(Some("test-user-email-2"), false)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "ApplicationsConnector.getApplication" - {
    "must" - {
      behave like successfulApplicationGetter(true)
    }

    "must" - {
      behave like successfulApplicationGetter(false)
    }

    "must return none when application is not found" in {
      stubFor(
        get(urlEqualTo("/api-hub-applications/applications/id-1"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).getApplication("id-1", enrich = true)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "ApplicationsConnector.deleteApplication" - {
    "must place the correct request" in {
      val id = "test-id"

      val userEmail = Some("me@test.com")
      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$id/delete"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(UserEmail(userEmail)).toString()))
          .willReturn(
            aResponse().withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).deleteApplication(id, userEmail)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
      }
    }

    "must return None when the application is not found" in {
      val id = "test-id"

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$id/delete"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).deleteApplication(id, None)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "ApplicationsConnector.testConnectivity" - {
    "must place the correct request and return the response" in {
      val expected = "something"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/test-connectivity"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(expected)
          )
      )

      buildConnector(this).testConnectivity()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }

    "must handle non-200 responses" in {

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/test-connectivity"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector(this).testConnectivity()(HeaderCarrier()) map {
        actual =>
          actual mustBe "Response status was 500"
      }
    }
  }

  "ApplicationsConnector.addCredential" - {
    "must place the correct request and return the new Credential" in {
      val expected = Credential("test-client-id", LocalDateTime.now(), Some("test-secret"), Some("test-fragment"))

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/${FakeApplication.id}/environments/primary/credentials"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector(this).addCredential(FakeApplication.id, Primary)(HeaderCarrier()) map {
        actual =>
          actual mustBe Right(Some(expected))
      }
    }

    "must return None if the application was not found" in {
      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/${FakeApplication.id}/environments/primary/credentials"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).addCredential(FakeApplication.id, Primary)(HeaderCarrier()) map {
        actual =>
          actual mustBe Right(None)
      }
    }

    "must transform a 409 Conflict response into an ApplicationCredentialLimitException" in {
      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/${FakeApplication.id}/environments/primary/credentials"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
          )
      )

      buildConnector(this).addCredential(FakeApplication.id, Primary)(HeaderCarrier()) map {
        actual =>
          actual mustBe Left(ApplicationCredentialLimitException.forId(FakeApplication.id, Primary))
      }
    }
  }

  "ApplicationsConnector.deleteCredential" - {
    "must place the correct request" in {
      val clientId = "test-client-id"

      stubFor(
        delete(urlEqualTo(s"/api-hub-applications/applications/${FakeApplication.id}/environments/primary/credentials/$clientId"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).deleteCredential(FakeApplication.id, Primary, clientId)(HeaderCarrier()) map {
        actual =>
          actual mustBe Right(Some(()))
      }
    }

    "must return None when the application does not exist" in {
      val clientId = "test-client-id"

      stubFor(
        delete(urlEqualTo(s"/api-hub-applications/applications/${FakeApplication.id}/environments/primary/credentials/$clientId"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).deleteCredential(FakeApplication.id, Primary, clientId)(HeaderCarrier()) map {
        actual =>
          actual mustBe Right(None)
      }
    }

    "must return ApplicationCredentialLimitException when an attempt is made to delete the last credential" in {
      val clientId = "test-client-id"

      stubFor(
        delete(urlEqualTo(s"/api-hub-applications/applications/${FakeApplication.id}/environments/primary/credentials/$clientId"))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
          )
      )

      buildConnector(this).deleteCredential(FakeApplication.id, Primary, clientId)(HeaderCarrier()) map {
        actual =>
          actual mustBe Left(ApplicationCredentialLimitException.forId(FakeApplication.id, Primary))
      }
    }
  }

  "ApplicationsConnector.createAccessRequest" - {
    "must place the correct request and return the new access requests" in {
      val request = AccessRequestRequest(
        applicationId = "test-application-id",
        supportingInformation = "test-supporting-information",
        requestedBy = "test-requested-by",
        apis = Seq(
          AccessRequestApi(
            apiId = "test-api-id",
            apiName = "test-api-name",
            endpoints = Seq(
              AccessRequestEndpoint(
                httpMethod = "test-http-method",
                path = "test-path",
                scopes = Seq("test-scope")
              )
            )
          )
        )
      )

      stubFor(
        post(urlEqualTo("/api-hub-applications/access-requests"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withRequestBody(equalToJson(Json.toJson(request).toString()))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
          )
      )

      buildConnector(this).createAccessRequest(request)(HeaderCarrier()).map {
        actual =>
          actual mustBe ()
      }
    }
  }

  "ApplicationsConnector.getAccessRequests" - {
    "must place the correct request and return the access requests" in {
      val accessRequest = AccessRequest(
        id = "test-id",
        applicationId = "test-application-id",
        apiId = "test-api-id",
        apiName = "test-api-name",
        status = Rejected,
        supportingInformation = "test-supporting-information",
        requested = LocalDateTime.now(),
        requestedBy = "test-requested-by"
      )

      val filters = Table(
        ("Application Id", "Status", "Query"),
        (Some("test-application-id"), Some(Rejected), "?applicationId=test-application-id&status=REJECTED"),
        (Some("test-application-id"), None, "?applicationId=test-application-id"),
        (None, Some(Rejected), "?status=REJECTED"),
        (None, None, "")
      )

      forAll(filters) {(applicationIdFilter: Option[String], statusFilter: Option[AccessRequestStatus], query: String) =>
        stubFor(
          get(urlEqualTo(s"/api-hub-applications/access-requests$query"))
            .withHeader(ACCEPT, equalTo("application/json"))
            .withHeader(AUTHORIZATION, equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(accessRequest)).toString())
            )
        )

        buildConnector(this).getAccessRequests(applicationIdFilter, statusFilter)(HeaderCarrier()).map {
          result =>
            result mustBe Seq(accessRequest)
        }
      }
    }
  }

  "ApplicationsConnector.getAccessRequest" - {
    "must place the correct request and return the access request when it exists" in {
      val accessRequest = AccessRequest(
        id = "test-id",
        applicationId = "test-application-id",
        apiId = "test-api-id",
        apiName = "test-api-name",
        status = Rejected,
        supportingInformation = "test-supporting-information",
        requested = LocalDateTime.now(),
        requestedBy = "test-requested-by"
      )

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/access-requests/${accessRequest.id}"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(accessRequest).toString())
          )
      )

      buildConnector(this).getAccessRequest(accessRequest.id)(HeaderCarrier()).map(
        result =>
          result mustBe Some(accessRequest)
      )
    }

    "must return None when the access request cannot be found" in {
      val id = "test-id"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/access-requests/$id"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).getAccessRequest(id)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

  "ApplicationsConnector.approveAccessRequest" - {
    "must place the correct request" in {
      val id = "test-id"
      val decisionRequest = AccessRequestDecisionRequest(decidedBy = "test-decided-by", rejectedReason = None)

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/access-requests/$id/approve"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(decisionRequest).toString()))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).approveAccessRequest(id, decisionRequest.decidedBy)(HeaderCarrier()).map(
        result =>
          result mustBe Some(())
      )
    }

    "must return None when the access request does not exist" in {
      val id = "test-id"
      val decisionRequest = AccessRequestDecisionRequest(decidedBy = "test-decided-by", rejectedReason = None)

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/access-requests/$id/approve"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).approveAccessRequest(id, decisionRequest.decidedBy)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

  "ApplicationsConnector.rejectAccessRequest" - {
    "must place the correct request" in {
      val id = "test-id"
      val decidedBy = "test-decided-by"
      val rejectedReason = "test-rejected-reason"
      val decisionRequest = AccessRequestDecisionRequest(decidedBy, Some(rejectedReason))

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/access-requests/$id/reject"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(decisionRequest).toString()))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).rejectAccessRequest(id, decidedBy, rejectedReason)(HeaderCarrier()).map(
        result =>
          result mustBe Some(())
      )
    }

    "must return None when the access request does not exist" in {
      val id = "test-id"
      val decidedBy = "test-decided-by"
      val rejectedReason = "test-rejected-reason"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/access-requests/$id/approve"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).rejectAccessRequest(id, decidedBy, rejectedReason)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

  "ApplicationsConnector.addApi" - {
    "must place the correct request" in {
      val applicationId = "test-id"
      val newApi = AddApiRequest("test-api-id", Seq(AddApiRequestEndpoint("GET", "test-path")), Seq("test-scope"))

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/applications/$applicationId/apis"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(newApi).toString()))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).addApi(applicationId, newApi)(HeaderCarrier()).map(
        result =>
          result mustBe Some(())
      )
    }

    "must return None when the application cannot be found" in {
      val applicationId = "test-id"
      val newApi = AddApiRequest("test-api-id", Seq(AddApiRequestEndpoint("GET", "test-path")), Seq("test-scope"))

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/applications/$applicationId/apis"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).addApi(applicationId, newApi)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

  "ApplicationsConnector.addTeamMember" - {
    "must place the correct request" in {
      val applicationId = "test-id"
      val teamMember = TeamMember("test-email")

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$applicationId/team-members"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(TeamMemberRequest(teamMember)).toString()))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).addTeamMember(applicationId, teamMember)(HeaderCarrier()).map(
        result =>
          result mustBe Some(())
      )
    }

    "must return None when the application cannot be found" in {
      val applicationId = "test-id"
      val teamMember = TeamMember("test-email")

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$applicationId/team-members"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).addTeamMember(applicationId, teamMember)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

  "ApplicationsConnector.generateDeployment" - {
    "must place the correct request and return the response" in {
      val request = DeploymentsRequest("test-lob", "test-name", "test-description", "test-egress", "test-team-id", "test-oas", false, "ALPHA")
      val response = SuccessfulDeploymentsResponse("test-id", "1.0.0", 102, "test-url")

      stubFor(
        post(urlEqualTo("/api-hub-applications/deployments"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(request).toString()))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
          )
      )

      buildConnector(this).generateDeployment(request)(HeaderCarrier()).map(
        result =>
          result mustBe response
      )
    }

    "must handle a 400 bad Request response with invalid OAS payload" in {
      val request = DeploymentsRequest("test-lob", "test-name", "test-description", "test-egress", "test-team-id", "test-oas", true, "BETA")

      stubFor(
        post(urlEqualTo("/api-hub-applications/deployments"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(Json.toJson(invalidOasResponse).toString())
          )
      )

      buildConnector(this).generateDeployment(request)(HeaderCarrier()).map(
        result =>
          result mustBe invalidOasResponse
      )
    }
  }

  "ApplicationsConnector.updateDeployment" - {
    "must place the correct request and return the response" in {
      val publisherRef = "test-publisher-ref"

      val request = RedeploymentRequest(
        description = "test-description",
        oas = "test-oas",
        status = "test-status"
      )

      val response = SuccessfulDeploymentsResponse("test-id", "1.0.0", 102, "test-url")

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/deployments/$publisherRef"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(request).toString()))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
          )
      )

      buildConnector(this).updateDeployment(publisherRef, request)(HeaderCarrier()).map(
        result =>
          result.value mustBe response
      )
    }

    "must handle a 400 bad Request response with invalid OAS payload" in {
      val publisherRef = "test-publisher-ref"

      val request = RedeploymentRequest(
        description = "test-description",
        oas = "test-oas",
        status = "test-status"
      )

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/deployments/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(Json.toJson(invalidOasResponse).toString())
          )
      )

      buildConnector(this).updateDeployment(publisherRef, request)(HeaderCarrier()).map(
        result =>
          result.value mustBe invalidOasResponse
      )
    }

    "must return None when a 404 Not Found is received" in {
      val publisherRef = "test-publisher-ref"

      val request = RedeploymentRequest(
        description = "test-description",
        oas = "test-oas",
        status = "test-status"
      )

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/deployments/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).updateDeployment(publisherRef, request)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

  "ApplicationsConnector.promoteToProduction" - {
    "must place the correct request and return SuccessfulDeploymentsResponse on success" in {
      val publisherRef = "test-publisher-ref"
      val response = SuccessfulDeploymentsResponse("test-id", "1.0.0", 102, "test-url")

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/deployments/$publisherRef/promote"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
          )
      )

      buildConnector(this).promoteToProduction(publisherRef)(HeaderCarrier()).map {
        result =>
          result.value mustBe response
      }
    }

    "must return InvalidOasResponse on failure" in {
      val publisherRef = "test-publisher-ref"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/deployments/$publisherRef/promote"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(Json.toJson(invalidOasResponse).toString())
          )
      )

      buildConnector(this).promoteToProduction(publisherRef)(HeaderCarrier()).map {
        result =>
          result.value mustBe invalidOasResponse
      }
    }

    "must return None when the service cannot be found" in {
      val publisherRef = "test-publisher-ref"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/deployments/$publisherRef/promote"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).promoteToProduction(publisherRef)(HeaderCarrier()).map {
        result =>
          result mustBe None
      }
    }
  }

  "ApplicationsConnector.getApiDeploymentStatuses" - {
    "must place the correct request and return the response" in {
      val publisherReference = "ref123"
      val response = Some(ApiDeploymentStatuses(Some("1.0"), None))

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/apis/$publisherReference/deployment-status"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
          )
      )

      buildConnector(this).getApiDeploymentStatuses(publisherReference)(HeaderCarrier()).map(
        result =>
          result mustBe response
      )
    }

    "must handle a 502 response" in {
      val publisherReference = "ref123"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/apis/$publisherReference/deployment-status"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector(this).getApiDeploymentStatuses(publisherReference)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }

    "must fail for unexpected responses" in {
      val publisherReference = "ref123"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/apis/$publisherReference/deployment-status"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = buildConnector(this).getApiDeploymentStatuses(publisherReference)(HeaderCarrier())
      recoverToExceptionIf[UpstreamErrorResponse](result).map { e =>
        e.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "ApplicationsConnector.findTeamById" - {
    "must place the correct request and return the team when it exists" in {
      val teamId = "test-team-id"
      val expected = Team(teamId, "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/teams/$teamId"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector(this).findTeamById(teamId)(HeaderCarrier()).map {
        result =>
          result mustBe Some(expected)
      }
    }

    "must place the request and return None when the team does not exist" in {
      val teamId = "test-team-id"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/teams/$teamId"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).findTeamById(teamId)(HeaderCarrier()).map {
        result =>
          result mustBe None
      }
    }
  }

  "ApplicationsConnector.findTeamByName" - {
    "must place the correct request and return the team when it exists" in {
      val name = "test-team-name"
      val expected = Team("test-team-id", name, LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/teams/name/$name"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector(this).findTeamByName(name)(HeaderCarrier()).map {
        result =>
          result mustBe Some(expected)
      }
    }

    "must place the request and return None when the team does not exist" in {
      val name = "test-team-name"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/teams/name/$name"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).findTeamByName(name)(HeaderCarrier()).map {
        result =>
          result mustBe None
      }
    }
  }

  "ApplicationsConnector.findTeams" - {
    "must place the correct request and return the matching teams when a user email is provided" in {
      val userEmail = "test-user-email"
      val expected = Seq(Team("id1", "team1", LocalDateTime.now(), Seq(TeamMember(userEmail))))
      val crypto = new ApplicationCrypto(ConfigFactory.parseResources("application.conf"))

      val userEmailEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(userEmail)).value
      val userEmailEncoded = URLEncoder.encode(userEmailEncrypted, "UTF-8")

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/teams?teamMember=$userEmailEncoded"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector(this).findTeams(Some(userEmail))(HeaderCarrier()).map {
        result =>
          result mustBe expected
      }
    }

    "must place the correct request and return the matching teams when no user email is provided" in {
      val expected = Seq(Team("id1", "team1", LocalDateTime.now(), Seq(TeamMember("test@example.com"))))

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/teams"))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector(this).findTeams(None)(HeaderCarrier()).map {
        result =>
          result mustBe expected
      }
    }
  }

  "ApplicationsConnector.createTeam" - {
    "must place the correct request and return the team created" in {
      val newTeam = NewTeam("test-team-name", Seq(TeamMember("test-email")))
      val team = Team("test-team-id", newTeam.name, LocalDateTime.now(), newTeam.teamMembers)

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/teams"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withHeader(ACCEPT, equalTo(ContentTypes.JSON))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withRequestBody(equalToJson(Json.toJson(newTeam).toString()))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(team).toString())
          )
      )

      buildConnector(this).createTeam(newTeam)(HeaderCarrier()).map {
        result =>
          result.value mustBe team
      }
    }

    "must return TeamNameNotUniqueException when the response is 409 Conflict" in {
      val newTeam = NewTeam("test-team-name", Seq(TeamMember("test-email")))

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/teams"))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
          )
      )

      buildConnector(this).createTeam(newTeam)(HeaderCarrier()).map {
        result =>
          result mustBe Left(TeamNameNotUniqueException.forName(newTeam.name))
      }
    }
  }

  "ApplicationsConnector.addTeamMemberToTeam" - {
    "must place the correct request" in {
      val teamId = "test-id"
      val teamMember = TeamMember("test-email")

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/teams/$teamId/members"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .withRequestBody(equalToJson(Json.toJson(TeamMemberRequest(teamMember)).toString()))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).addTeamMemberToTeam(teamId, teamMember)(HeaderCarrier()).map(
        result =>
          result mustBe Some(())
      )
    }

    "must return None when the application cannot be found" in {
      val teamId = "test-id"
      val teamMember = TeamMember("test-email")

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/teams/$teamId/members"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).addTeamMemberToTeam(teamId, teamMember)(HeaderCarrier()).map(
        result =>
          result mustBe None
      )
    }
  }

}

object ApplicationsConnectorSpec extends HttpClientV2Support {

  def buildConnector(wireMockSupport: WireMockSupport)(implicit ec: ExecutionContext): ApplicationsConnector = {
    val servicesConfig = new ServicesConfig(
      Configuration.from(Map(
        "microservice.services.api-hub-applications.host" -> wireMockSupport.wireMockHost,
        "microservice.services.api-hub-applications.port" -> wireMockSupport.wireMockPort
      ))
    )

    val application = new GuiceApplicationBuilder().build()
    val crypto: ApplicationCrypto = application.injector.instanceOf[ApplicationCrypto]
    new ApplicationsConnector(httpClientV2, crypto, servicesConfig, application.injector.instanceOf[FrontendAppConfig])
  }

  def toJsonString(newApplication: NewApplication): String = {
    Json.toJson(newApplication).toString()
  }

  def toJsonString(application: Application): String = {
    Json.toJson(application).toString()
  }

  def toJsonString(applications: Seq[Application]): String = {
    Json.toJson(applications).toString()
  }

  object FakeUser extends UserModel("id", "test-name", LdapUser, Some("test-email"))

  object FakeApplication extends Application(
    "fake-application-id",
    "fake-application-name",
    LocalDateTime.now(),
    Creator(FakeUser.email.get),
    LocalDateTime.now(),
    Seq(TeamMember(FakeUser.email.get)),
    Environments(),
    Seq.empty
  )

  val invalidOasResponse: InvalidOasResponse = InvalidOasResponse(
    FailuresResponse(
      code = "BAD_REQUEST",
      reason = "Validation Failed.",
      errors = Some(Seq(Error("METADATA", """name must match \"^[a-z0-9\\-]+$\"""")))
    )
  )

  trait ApplicationGetterBehaviours {
    this: AsyncFreeSpec with Matchers with WireMockSupport =>

    def successfulApplicationGetter(enrich: Boolean): Unit = {
      s"must place the correct request and return the application when enrich = $enrich" in {
        val api = Api("api_id", Seq(SelectedEndpoint("GET", "/foo/bar")))
        val applicationWithApis = Application(
          "id-1",
          "test-name-1",
          Creator("test-creator-email-1"),
          Seq(TeamMember("test-creator-email-1"))).copy(apis = Seq(api)
        )

        val expectedJson = toJsonString(applicationWithApis)

        stubFor(
          get(urlEqualTo(s"/api-hub-applications/applications/id-1?enrich=$enrich"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(expectedJson)
            )
        )

        buildConnector(this).getApplication("id-1", enrich)(HeaderCarrier()) map {
          actual =>
            actual mustBe Some(applicationWithApis)
        }
      }

    }

  }

}
