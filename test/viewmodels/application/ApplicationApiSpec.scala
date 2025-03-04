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

package viewmodels.application

import config.{BaseHipEnvironment, DefaultHipEnvironment, HipEnvironment}
import controllers.actions.FakeApiDetail
import fakes.FakeHipEnvironments
import models.accessrequest.{AccessRequest, AccessRequestEndpoint, AccessRequestStatus, Pending, Approved, Rejected}
import models.api.*
import models.application.{Api, SelectedEndpoint}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Instant, LocalDateTime}

class ApplicationApiSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  import ApplicationApiSpec.*

  private val selectedEndpoint = SelectedEndpoint(httpMethod = "test-method", path = "test-path")
  private def buildApplicationEndpoint(scopes: Seq[String] = Seq.empty,
                                       approvedScopes: Map[String,Set[String]] = Map.empty,
                                       pendingRequests: Seq[AccessRequest] = Seq.empty) = {
    ApplicationEndpoint(
      httpMethod = selectedEndpoint.httpMethod,
      path = selectedEndpoint.path,
      summary = None,
      description = None,
      scopes = scopes,
      theoreticalScopes = TheoreticalScopes(scopes.toSet, approvedScopes),
      pendingAccessRequests = pendingRequests
    )
  }

  private def buildPendingAccessRequest(environmentId: String) = {
    buildAccessRequest(Pending, environmentId)
  }

  private def buildAccessRequest(status: AccessRequestStatus, environmentId: String, endpoints: Seq[AccessRequestEndpoint] = Seq.empty) = {
    AccessRequest(
      id = "test-id",
      applicationId = "test-application-id",
      apiId = "test-api-id",
      apiName = "test-api-name",
      status = status,
      supportingInformation = "test-supporting-information",
      requested = LocalDateTime.now(),
      requestedBy = "requester",
      environmentId = environmentId
    ).copy(endpoints = endpoints)
  }

  private def buildApplicationApi(endpoints: Seq[ApplicationEndpoint] = Seq.empty, pendingAccessRequests: Seq[AccessRequest] = Seq.empty) = {
    ApplicationApi(
      apiId = "test-api-id",
      apiTitle = "test-api-title",
      endpoints = endpoints,
      pendingAccessRequests = pendingAccessRequests,
      isMissing = false
    )
  }
  
  private def buildAccessRequestEndpoint(scopes: Seq[String]) = {
    AccessRequestEndpoint(httpMethod = "GET", path = "/path", scopes = scopes)
  }

  "ApplicationEndpoint" - {
    "accessFor must return Unknown if scopes are empty" in {
      buildApplicationEndpoint(scopes = Seq.empty).accessFor(FakeHipEnvironments.production) mustBe Unknown
    }

    "accessFor must return Accessible for non-prod-like environments even if there are no approvals" in {
      buildApplicationEndpoint(
        scopes = Seq("scope1", "scope2"),
        approvedScopes = Map.empty
      ).accessFor(FakeHipEnvironments.test) mustBe Accessible
    }

    "accessFor must return Accessible for prod-like environments if there are sufficient approvals" in {
      buildApplicationEndpoint(
        scopes = Seq("scope1", "scope2"),
        approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope1", "scope2"))
      ).accessFor(FakeHipEnvironments.production) mustBe Accessible
    }

    "accessFor must return Requested for prod-like environments if there are insufficient approvals but there are pending requests" in {
      buildApplicationEndpoint(
        scopes = Seq("scope1", "scope2"),
        approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope1")),
        pendingRequests = Seq(buildPendingAccessRequest(FakeHipEnvironments.production.id))
      ).accessFor(FakeHipEnvironments.production) mustBe Requested
    }

    "accessFor must return Inaccessible for prod-like environments if there are insufficient approvals and no pending requests" in {
      buildApplicationEndpoint(
        scopes = Seq("scope1", "scope2"),
        approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope1")),
        pendingRequests = Seq.empty
      ).accessFor(FakeHipEnvironments.production) mustBe Inaccessible
    }

    "must construct the correct endpoint for a missing API" in {
      val actual = ApplicationEndpoint.forMissingApi(selectedEndpoint)
      val expected = ApplicationEndpoint(
        httpMethod = selectedEndpoint.httpMethod,
        path = selectedEndpoint.path,
        summary = None,
        description = None,
        scopes = Seq.empty,
        theoreticalScopes = TheoreticalScopes(Set.empty, Map.empty),
        pendingAccessRequests = Seq.empty
      )

      actual mustBe expected
    }
  }

  "ApplicationApi" - {
    val unknownEndpoint = buildApplicationEndpoint(scopes = Seq.empty)
    val requestedEndpoint = buildApplicationEndpoint(scopes = Seq("scope1"), pendingRequests = Seq(buildPendingAccessRequest(FakeHipEnvironments.production.id)))
    val accessibleEndpoint1 = buildApplicationEndpoint(scopes = Seq("scope1", "scope2"), approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope1", "scope2")))
    val accessibleEndpoint2 = buildApplicationEndpoint(scopes = Seq("scope3"), approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope3")))
    val inaccessibleEndpoint = buildApplicationEndpoint(scopes = Seq("scope1", "scope2"), approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope1")))

    "selectedEndpoints" - {
      "must return correct value for multiple endpoints" in {
        buildApplicationApi(Seq(
          buildApplicationEndpoint(scopes = Seq("scope1", "scope2")),
          buildApplicationEndpoint(scopes = Seq("scope3"))
        )).selectedEndpoints mustBe 2
      }
      "must return correct value for no endpoints" in {
        buildApplicationApi().selectedEndpoints mustBe 0
      }
    }

    "availableEndpoints" - {
      "must return correct number of accessible endpoints in matching environment" in {
        buildApplicationApi(Seq(
          unknownEndpoint,
          requestedEndpoint,
          accessibleEndpoint1,
          accessibleEndpoint2,
          inaccessibleEndpoint
        )).availableEndpoints(FakeHipEnvironments.production) mustBe 2
      }

      "must return 0 for accessible endpoints in non-matching environment" in {
        buildApplicationApi(Seq(
          unknownEndpoint,
          requestedEndpoint,
          accessibleEndpoint1,
          accessibleEndpoint2,
          inaccessibleEndpoint
        )).availableEndpoints(FakeHipEnvironments.test) mustBe 0
      }
    }

    "needsAccessRequest" - {
      "must return true if there are inaccessible endpoints and no pending requests" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1,
            inaccessibleEndpoint
          ),
          pendingAccessRequests = Seq.empty
        ).needsAccessRequest(FakeHipEnvironments.production) mustBe true
      }

      "must return false if there are inaccessible endpoints but not for this environment" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1,
            inaccessibleEndpoint
          ),
          pendingAccessRequests = Seq.empty
        ).needsAccessRequest(FakeHipEnvironments.test) mustBe false
      }

      "must return false if there are no inaccessible endpoints" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1
          ),
          pendingAccessRequests = Seq.empty
        ).needsAccessRequest(FakeHipEnvironments.production) mustBe false
      }

      "must return false if there are no inaccessible endpoints and there are pending requests" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1
          ),
          pendingAccessRequests = Seq(
            buildPendingAccessRequest(FakeHipEnvironments.production.id)
          )
        ).needsAccessRequest(FakeHipEnvironments.production) mustBe false
      }
    }

    "hasPendingAccessRequest" - {
      "must return true if there are pending requests for this environment" in {
        buildApplicationApi(
          pendingAccessRequests = Seq(
            buildPendingAccessRequest(FakeHipEnvironments.production.id)
          )
        ).hasPendingAccessRequest(FakeHipEnvironments.production) mustBe true
      }

      "must return false if there are pending requests but not for this environment" in {
        buildApplicationApi(
          pendingAccessRequests = Seq(
            buildPendingAccessRequest(FakeHipEnvironments.production.id)
          )
        ).hasPendingAccessRequest(FakeHipEnvironments.test) mustBe false
      }

      "must return false if there are no pending requests" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1
          ),
          pendingAccessRequests = Seq.empty
        ).hasPendingAccessRequest(FakeHipEnvironments.production) mustBe false
      }
    }

    "isAccessibleInEnvironment" - {
      "must return true if there are accessible endpoints in this environments" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1
          )
        ).isAccessibleInEnvironment(FakeHipEnvironments.production) mustBe true
      }

      "must return false if there are accessible endpoints but not in this environments" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            accessibleEndpoint1
          )
        ).isAccessibleInEnvironment(FakeHipEnvironments.test) mustBe true
      }

      "must return false if there are no accessible endpoints" in {
        buildApplicationApi(
          endpoints = Seq(
            unknownEndpoint,
            requestedEndpoint,
            inaccessibleEndpoint
          )
        ).isAccessibleInEnvironment(FakeHipEnvironments.production) mustBe false
      }
    }

    "must construct the correct object for a missing API" in {
      val api = Api(
        id = "test-id",
        title = "test-title",
        endpoints = Seq(
          SelectedEndpoint(httpMethod = "test-method-1", path = "test-path-1"),
          SelectedEndpoint(httpMethod = "test-method-2", path = "test-path-2")
        )
      )

      val actual = ApplicationApi(api, Seq(buildPendingAccessRequest(FakeHipEnvironments.production.id)))

      val expected = ApplicationApi(
        apiId = api.id,
        apiTitle = api.title,
        endpoints = api.endpoints.map(ApplicationEndpoint.forMissingApi),
        pendingAccessRequests = actual.pendingAccessRequests,
        isMissing = true
      )

      actual mustBe expected
    }

}

  "TheoreticalScopes" - {
    "allowedScopes" - {
      val theoreticalScopes = TheoreticalScopes(
        requiredScopes = Set("scope1", "scope2", "scope3"),
        approvedScopes = Map(FakeHipEnvironments.production.id -> Set("scope1", "scope2", "scope4"))
      )

      "returns correct scopes for matching environment" in {
        theoreticalScopes.allowedScopes(FakeHipEnvironments.production) mustBe Set("scope1", "scope2")
      }

      "returns empty set for non-matching environment" in {
        theoreticalScopes.allowedScopes(FakeHipEnvironments.test) mustBe Set.empty
      }
    }

    "apply" - {
      "constructs requiredScopes correctly" in {
        val api1 = Api(id = "api1", title = "title1")
        val api2 = Api(id = "api2", title = "title2")
        val api3 = Api(id = "api3", title = "title3")
        val apiDetail1 = FakeApiDetail.copy(endpoints = Seq(Endpoint("/path1", Seq(EndpointMethod("GET", None, None, Seq("scope1", "scope2"))))))
        val apiDetail2 = FakeApiDetail.copy(endpoints = Seq(Endpoint("/path2", Seq(EndpointMethod("GET", None, None, Seq("scope2", "scope3"))))))

        TheoreticalScopes(Seq(
          (api1, Some(apiDetail1)),
          (api2, Some(apiDetail2)),
          (api3, None),
        ), Seq.empty).requiredScopes mustBe Set("scope1", "scope2", "scope3")
      }

      "constructs approvedScopes correctly" in {
        TheoreticalScopes(Seq.empty, Seq(
          buildAccessRequest(Pending, FakeHipEnvironments.production.id, Seq(buildAccessRequestEndpoint(Seq("scope1", "scope2")))),
          buildAccessRequest(Approved, FakeHipEnvironments.production.id, Seq(buildAccessRequestEndpoint(Seq("scope3", "scope4")))),
          buildAccessRequest(Rejected, FakeHipEnvironments.production.id, Seq(buildAccessRequestEndpoint(Seq("scope5", "scope6")))),
          buildAccessRequest(Approved, FakeHipEnvironments.production.id, Seq(buildAccessRequestEndpoint(Seq("scope7", "scope8")))),
          buildAccessRequest(Rejected, FakeHipEnvironments.test.id, Seq(buildAccessRequestEndpoint(Seq("scope9", "scope10")))),
          buildAccessRequest(Approved, FakeHipEnvironments.test.id, Seq(buildAccessRequestEndpoint(Seq("scope11", "scope12")))),
        )).approvedScopes mustBe Map(
          FakeHipEnvironments.production.id -> Set("scope3", "scope4", "scope7", "scope8"),
          FakeHipEnvironments.test.id -> Set("scope11", "scope12")
        )
    }
  }
}
