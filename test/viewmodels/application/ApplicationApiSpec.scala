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
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Approved, Primary, Scope, Secondary}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ApplicationApiSpec extends AnyFreeSpec with Matchers {

  import ApplicationApiSpec._

  "ApplicationEndpointAccess" - {
    "must return Accessible when an application has the scopes required by the endpoint" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1, testScope2, testScope3))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope1, testScope3))
      val actual = ApplicationEndpointAccess(application, endpointMethod, Primary)

      actual mustBe Accessible
    }

    "must return Inaccessible when an application does not have the scopes required by the endpoint" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope2, testScope3))
      val actual = ApplicationEndpointAccess(application, endpointMethod, Primary)

      actual mustBe Inaccessible
    }

    "must return Inaccessible when an application only has a subset of the scopes required by the endpoint" in {
      val application = FakeApplication.setPrimaryScopes(scopes(testScope1, testScope2))
      val endpointMethod = EndpointMethod("GET", None, None, Seq(testScope2, testScope3))
      val actual = ApplicationEndpointAccess(application, endpointMethod, Primary)

      actual mustBe Inaccessible
    }

    "must return Inaccessible when an application has the scopes required but in the wrong environment" in {
      val application = FakeApplication.setPrimaryScopes(scopes("test-scope-1", "test-scope-2", "test-scope-3"))
      val endpointMethod = EndpointMethod("GET", None, None, Seq("test-scope-1", "test-scope-3"))
      val actual = ApplicationEndpointAccess(application, endpointMethod, Secondary)

      actual mustBe Inaccessible
    }
  }

  "ApplicationApi" - {
    "must produce the correct summary values" in {
      val apiDetail = ApiDetail(
        id = "test-id",
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
        openApiSpecification = "test-oas-spec"
      )

      val endpoints = Seq(
        ApplicationEndpoint(
          httpMethod = "GET",
          path = "/test",
          scopes = Seq.empty,
          primaryAccess = Accessible,
          secondaryAccess = Accessible
        ),
        ApplicationEndpoint(
          httpMethod = "GET",
          path = "/test",
          scopes = Seq.empty,
          primaryAccess = Accessible,
          secondaryAccess = Inaccessible
        ),
        ApplicationEndpoint(
          httpMethod = "GET",
          path = "/test",
          scopes = Seq.empty,
          primaryAccess = Inaccessible,
          secondaryAccess = Accessible
        ),
        ApplicationEndpoint(
          httpMethod = "GET",
          path = "/test",
          scopes = Seq.empty,
          primaryAccess = Requested,
          secondaryAccess = Accessible
        )
      )

      val applicationApi = ApplicationApi(apiDetail, endpoints)

      applicationApi.selectedEndpoints mustBe 4
      applicationApi.totalEndpoints mustBe 3
      applicationApi.availablePrimaryEndpoints mustBe 2
      applicationApi.availableSecondaryEndpoints mustBe 3
    }
  }

}

object ApplicationApiSpec {

  private val testScope1 = "test-scope-1"
  private val testScope2 = "test-scope-2"
  private val testScope3 = "test-scope-3"

  private def scope(name: String): Scope = {
    Scope(name, Approved)
  }

  private def scopes(name: String *): Seq[Scope] = {
    name.map(scope)
  }

}
