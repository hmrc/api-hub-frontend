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

package models.api

import generators.ApiDetailGenerators
import models.api.ApiDetailLenses.ApiDetailLensOps
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ApiDetailLensesSpec extends AnyFreeSpec with Matchers {

  import ApiDetailLensesSpec._

  "getRequiredScopeNames" - {
    "must return the set of all endpoint scopes" in {
      val endpoints = Seq(
        Endpoint(
          "/etc",
          Seq(
            EndpointMethod("GET", None, None, Seq("test-scope-1", "test-scope-2")),
            EndpointMethod("POST", None, None, Seq("test-scope-2", "test-scope-3")),
            EndpointMethod("DELETE", None, None, Seq.empty)
          )
        ),
        Endpoint(
          "/foo",
          Seq(
            EndpointMethod("GET", None, None, Seq("test-scope-3", "test-scope-4"))
          )
        ),
        Endpoint(
          "/bar",
          Seq(
            EndpointMethod("GET", None, None, Seq.empty)
          )
        ),
        Endpoint(
          "/foobar",
          Seq.empty
        )
      )

      val apiDetail = testApiDetail.copy(endpoints = endpoints)

      val actual = apiDetail.getRequiredScopeNames
      actual must contain theSameElementsAs Set("test-scope-1", "test-scope-2", "test-scope-3", "test-scope-4")
    }

    "must return an empty set when there are no endpoint scopes" in {
      val apiDetail = testApiDetail

      val actual = apiDetail.getRequiredScopeNames
      actual mustBe empty
    }
  }

}

object ApiDetailLensesSpec extends ApiDetailGenerators {

  val testApiDetail: ApiDetail = sampleApiDetail().copy(endpoints = Seq.empty)

}
