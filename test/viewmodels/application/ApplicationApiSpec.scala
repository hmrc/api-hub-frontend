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

import controllers.actions.FakeApplication
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live, Maintainer}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Api, Primary, Scope, Secondary, SelectedEndpoint}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant

class ApplicationApiSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  import ApplicationApiSpec._

  "ApplicationEndpointAccess" - {
    "must return Accessible when an application has the scopes required by the endpoint" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1, testScope2, testScope3))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope1, testScope3))
      val actual = ApplicationEndpointAccess(application, 0, endpointMethod, Primary)

      actual mustBe Accessible
    }

    "must return Inaccessible when an application does not have the scopes required by the endpoint" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope2, testScope3))
      val actual = ApplicationEndpointAccess(application, 0, endpointMethod, Primary)

      actual mustBe Inaccessible
    }

    "must return Inaccessible when an application only has a subset of the scopes required by the endpoint" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1, testScope2))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope2, testScope3))
      val actual = ApplicationEndpointAccess(application, 0, endpointMethod, Primary)

      actual mustBe Inaccessible
    }

    "must return Inaccessible when an application has the scopes required but in the wrong environment" in {
      val application = FakeApplication.setPrimaryScopes(scopes("test-scope-1", "test-scope-2", "test-scope-3"))
      val endpointMethod = EndpointMethod("GET", None, None, Seq("test-scope-1", "test-scope-3"))
      val actual = ApplicationEndpointAccess(application, 0, endpointMethod, Secondary)

      actual mustBe Inaccessible
    }

    "must return Requested for a primary endpoint without required scopes when there is a pending production access request" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope2, testScope3))
      val actual = ApplicationEndpointAccess(application, 1, endpointMethod, Primary)

      actual mustBe Requested
    }

    "must return Accessible for a primary endpoint with required scopes when there is a pending production access request" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1, testScope2, testScope3))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope1, testScope3))
      val actual = ApplicationEndpointAccess(application, 1, endpointMethod, Primary)

      actual mustBe Accessible
    }
  }

  "ApplicationEndpoint" - {
    "must construct the correct endpoint for a missing API" in {
      val selectedEndpoint = SelectedEndpoint(httpMethod = "test-method", path = "test-path")
      val actual = ApplicationEndpoint.forMissingApi(selectedEndpoint)
      val expected = ApplicationEndpoint(
        httpMethod = selectedEndpoint.httpMethod,
        path = selectedEndpoint.path,
        summary = None,
        description = None,
        scopes = Seq.empty,
        primaryAccess = Unknown,
        secondaryAccess = Unknown
      )

      actual mustBe expected
    }
  }

  "ApplicationApi" - {
    "must produce the correct summary values" in {
      Range.inclusive(0,1) map (pendingAccessRequestCount =>
        val apiDetail = ApiDetail(
          id = "test-id",
          publisherReference = "test-pub-ref",
          title = "test-title",
          description = "test-description",
          version = "test-version",
          endpoints = Seq(
            Endpoint(
              path = "/test",
              methods = Seq(
                EndpointMethod("GET", None, None, Seq.empty),
                EndpointMethod("POST", None, None, Seq.empty)
              )
            ),
            Endpoint(
              path = "/test2",
              methods = Seq(
                EndpointMethod("GET", None, None, Seq.empty)
              )
            )
          ),
          shortDescription = None,
          openApiSpecification = "test-oas-spec",
          apiStatus = Live,
          reviewedDate = Instant.now(),
          platform = "HIP",
          maintainer = Maintainer("name", "#slack", List.empty)
        )

        val endpoints = Seq(
          ApplicationEndpoint(
            httpMethod = "GET",
            path = "/test",
            summary = Some("test-summary"),
            description = Some("test-description"),
            scopes = Seq.empty,
            primaryAccess = Accessible,
            secondaryAccess = Accessible
          ),
          ApplicationEndpoint(
            httpMethod = "GET",
            path = "/test",
            summary = Some("test-summary"),
            description = Some("test-description"),
            scopes = Seq.empty,
            primaryAccess = Accessible,
            secondaryAccess = Inaccessible
          ),
          ApplicationEndpoint(
            httpMethod = "GET",
            path = "/test",
            summary = Some("test-summary"),
            description = Some("test-description"),
            scopes = Seq.empty,
            primaryAccess = Inaccessible,
            secondaryAccess = Accessible
          ),
          ApplicationEndpoint(
            httpMethod = "GET",
            path = "/test",
            summary = Some("test-summary"),
            description = Some("test-description"),
            scopes = Seq.empty,
            primaryAccess = Requested,
            secondaryAccess = Accessible
          )
        )

        val applicationApi = ApplicationApi(apiDetail, endpoints, pendingAccessRequestCount)

        applicationApi.selectedEndpoints mustBe 4
        applicationApi.totalEndpoints mustBe 3
        applicationApi.availablePrimaryEndpoints mustBe 2
        applicationApi.availableSecondaryEndpoints mustBe 3
        applicationApi.needsProductionAccessRequest mustBe !applicationApi.hasPendingAccessRequest )
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

      val actual = ApplicationApi(api, 1)

      val expected = ApplicationApi(
        apiId = api.id,
        apiTitle = api.title,
        totalEndpoints = 0,
        endpoints = api.endpoints.map(ApplicationEndpoint.forMissingApi),
        pendingAccessRequestCount = 1,
        isMissing = true
      )

      actual mustBe expected
    }
}

object ApplicationApiSpec {

  private val testScope1 = "test-scope-1"
  private val testScope2 = "test-scope-2"
  private val testScope3 = "test-scope-3"

  private def scope(name: String): Scope = {
    Scope(name)
  }

  private def scopes(name: String *): Seq[Scope] = {
    name.map(scope)
  }

}
