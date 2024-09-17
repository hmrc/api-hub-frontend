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

package models

import controllers.actions.FakeApplication
import models.api.ApiDetailLensesSpec.sampleOas
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live, Maintainer}
import models.application.{Api, SelectedEndpoint}
import models.application.ApplicationLenses._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.time.Instant

class AvailableEndpointsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  private val testApiDetail = ApiDetail(
    id = "test-id",
    publisherReference = "test-publisher-reference",
    title = "test-title",
    description = "test-description",
    version = "test-version",
    endpoints = Seq.empty,
    shortDescription = None,
    openApiSpecification = sampleOas,
    apiStatus = Live,
    reviewedDate = Instant.now(),
    platform = "HIP",
    maintainer = Maintainer("test-name", "test-slack"),
  )

  "AvailableEndpoints.apply" - {
    "must group endpoint methods by the same set of scopes" in {
      val scopeSets = Table(
        "Scopes",
        Seq.empty,
        Seq("test-scopes-1"),
        Seq("test-scopes-1", "test-scopes-2"),
        Seq("test-scopes-1", "test-scopes-2", "test-scopes-3")
      )

      forAll(scopeSets) {scopes =>
        val path1 = "/test-path-1"
        val path2 = "/test-path-2"

        val endpoint1method1 = EndpointMethod(
          httpMethod = "GET",
          summary = None,
          description = None,
          scopes = scopes
        )

        val endpoint1method2 = EndpointMethod(
          httpMethod = "POST",
          summary = None,
          description = None,
          scopes = scopes
        )

        val endpoint2method1 = EndpointMethod(
          httpMethod = "GET",
          summary = None,
          description = None,
          scopes = scopes
        )

        val apiDetail = testApiDetail.copy(
          endpoints = Seq(
            Endpoint(
              path = path1,
              methods = Seq(endpoint1method1, endpoint1method2)
            ),
            Endpoint(
              path = path2,
              methods = Seq(endpoint2method1)
            )
          )
        )

        val expected = Map(
          scopes.toSet -> Seq(
            AvailableEndpoint(path1, endpoint1method1, false),
            AvailableEndpoint(path1, endpoint1method2, false),
            AvailableEndpoint(path2, endpoint2method1, false)
          )
        )

        val actual = AvailableEndpoints(apiDetail, FakeApplication)

        actual mustBe expected
      }
    }

    "must group endpoints by different sets of scopes" in {
      val scopeSets = Table(
        ("Scopes1", "Scopes2"),
        (Seq("test-scopes-1"), Seq.empty),
        (Seq("test-scopes-1"), Seq("test-scopes-2")),
        (Seq("test-scopes-1"), Seq("test-scopes-1", "test-scopes-2")),
        (Seq("test-scopes-1"), Seq("test-scopes-1", "test-scopes-2", "test-scopes-3"))
      )

      forAll(scopeSets) {(scopes1, scopes2) =>
        val path1 = "/test-path-1"
        val path2 = "/test-path-2"
        val path3 = "/test-path-3"

        val endpoint1method1 = EndpointMethod(
          httpMethod = "GET",
          summary = None,
          description = None,
          scopes = scopes1
        )

        val endpoint1method2 = EndpointMethod(
          httpMethod = "POST",
          summary = None,
          description = None,
          scopes = scopes2
        )

        val endpoint2method1 = EndpointMethod(
          httpMethod = "GET",
          summary = None,
          description = None,
          scopes = scopes1
        )

        val endpoint3method1 = EndpointMethod(
          httpMethod = "GET",
          summary = None,
          description = None,
          scopes = scopes2
        )

        val apiDetail = testApiDetail.copy(
          endpoints = Seq(
            Endpoint(
              path = path1,
              methods = Seq(endpoint1method1, endpoint1method2)
            ),
            Endpoint(
              path = path2,
              methods = Seq(endpoint2method1)
            ),
            Endpoint(
              path = path3,
              methods = Seq(endpoint3method1)
            )
          )
        )

        val expected = Map(
          scopes1.toSet -> Seq(
            AvailableEndpoint(path1, endpoint1method1, false),
            AvailableEndpoint(path2, endpoint2method1, false)
          ),
          scopes2.toSet -> Seq(
            AvailableEndpoint(path1, endpoint1method2, false),
            AvailableEndpoint(path3, endpoint3method1, false)
          )
        )

        val actual = AvailableEndpoints(apiDetail, FakeApplication)

        actual mustBe expected
      }
    }
  }

  "selectedEndpoints" - {
    "must return the correct endpoints for the selected scopes" in {
      val path1 = "/test-path-1"
      val path2 = "/test-path-2"
      val path3 = "/test-path-3"
      val path4 = "/test-path-4"

      val scopes1 = Seq("test-scopes-1", "test-scopes-2")
      val scopes2 = Seq("test-scopes-1", "test-scopes-3")

      val endpoint1method1 = EndpointMethod(
        httpMethod = "GET",
        summary = None,
        description = None,
        scopes = scopes1
      )

      val endpoint1method2 = EndpointMethod(
        httpMethod = "POST",
        summary = None,
        description = None,
        scopes = scopes2
      )

      val endpoint1method3 = EndpointMethod(
        httpMethod = "PUT",
        summary = None,
        description = None,
        scopes = Seq("test-scopes-1", "no-match")
      )

      val endpoint2method1 = EndpointMethod(
        httpMethod = "GET",
        summary = None,
        description = None,
        scopes = scopes1
      )

      val endpoint3method1 = EndpointMethod(
        httpMethod = "POST",
        summary = None,
        description = None,
        scopes = scopes2
      )

      val endpoint4method1 = EndpointMethod(
        httpMethod = "POST",
        summary = None,
        description = None,
        scopes = Seq("test-scopes-1", "no-match")
      )

      val apiDetail = testApiDetail.copy(
        endpoints = Seq(
          Endpoint(
            path = path1,
            methods = Seq(endpoint1method1, endpoint1method2, endpoint1method3)
          ),
          Endpoint(
            path = path2,
            methods = Seq(endpoint2method1)
          ),
          Endpoint(
            path = path3,
            methods = Seq(endpoint3method1)
          ),
          Endpoint(
            path = path4,
            methods = Seq(endpoint4method1)
          )
        )
      )

      val actual = AvailableEndpoints.selectedEndpoints(apiDetail, FakeApplication, Set(scopes1.toSet, scopes2.toSet))

      val expected = Map(
        scopes1.toSet -> Seq(AvailableEndpoint(path1, endpoint1method1, false), AvailableEndpoint(path2, endpoint2method1, false)),
        scopes2.toSet -> Seq(AvailableEndpoint(path1, endpoint1method2, false), AvailableEndpoint(path3, endpoint3method1, false))
      )

      actual mustBe expected
    }

    "must return an empty map when the selected scopes do not match any endpoints" in {
      val path1 = "/test-path-1"

      val scopes1 = Seq("test-scopes-1", "test-scopes-2")

      val endpoint1method1 = EndpointMethod(
        httpMethod = "GET",
        summary = None,
        description = None,
        scopes = scopes1
      )

      val apiDetail = testApiDetail.copy(
        endpoints = Seq(
          Endpoint(
            path = path1,
            methods = Seq(endpoint1method1)
          )
        )
      )

      val actual = AvailableEndpoints.selectedEndpoints(apiDetail, FakeApplication, Set(Set("no-match")))

      actual mustBe empty
    }

    "must return an empty map for an empty set of selected scopes" in {
      val path1 = "/test-path-1"

      val scopes1 = Seq("test-scopes-1", "test-scopes-2")

      val endpoint1method1 = EndpointMethod(
        httpMethod = "GET",
        summary = None,
        description = None,
        scopes = scopes1
      )

      val apiDetail = testApiDetail.copy(
        endpoints = Seq(
          Endpoint(
            path = path1,
            methods = Seq(endpoint1method1)
          )
        )
      )

      val actual = AvailableEndpoints.selectedEndpoints(apiDetail, FakeApplication, Set.empty[Set[String]])

      actual mustBe empty
    }
  }

  "AvailableEndpoint.apply" - {
    val endpointMethod1 = EndpointMethod("GET", None, None, Seq("test-scope-1"))
    val endpointMethod2 = EndpointMethod("GET", None, None, Seq("test-scope-2"))

    val endpoint1 = Endpoint("/test-path-1", Seq(endpointMethod1))
    val endpoint2 = Endpoint("/test-path-2", Seq(endpointMethod2))

    val apiDetail = testApiDetail.copy(
      endpoints = Seq(
        endpoint1,
        endpoint2
      )
    )

    val application = FakeApplication.addApi(
      Api(apiDetail.id, apiDetail.title, Seq(SelectedEndpoint("GET", "/test-path-1")))
    )

    "must set the added attribute correctly when the endpoint has been added to the application" in {
      val actual = AvailableEndpoint.apply("/test-path-1", endpointMethod1, apiDetail, application)

      actual mustBe AvailableEndpoint("/test-path-1", endpointMethod1, true)
    }

    "must set the added attribute correctly when the endpoint has not been added to the application" in {
      val actual = AvailableEndpoint.apply("/test-path-2", endpointMethod2, apiDetail, application)

      actual mustBe AvailableEndpoint("/test-path-2", endpointMethod2, false)
    }
  }

}
