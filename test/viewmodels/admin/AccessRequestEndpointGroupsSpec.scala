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

package viewmodels.admin

import generators.AccessRequestGenerator
import models.accessrequest.AccessRequestEndpoint
import models.accessrequest.AccessRequestLenses._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AccessRequestEndpointGroupsSpec extends AnyFreeSpec with Matchers with AccessRequestGenerator {

  "AccessRequestEndpointGroups.group" - {
    "must correctly group an access request's endpoints by scope" in {
      val scopesA = Seq("test-scope-1")
      val scopesB = Seq("test-scope-1", "test-scope-2")

      val endpoint1 = AccessRequestEndpoint(
        httpMethod = "test-method-1",
        path = "test-path-1",
        scopes = scopesA
      )

      val endpoint2 = AccessRequestEndpoint(
        httpMethod = "test-method-2",
        path = "test-path-2",
        scopes = scopesA
      )

      val endpoint3 = AccessRequestEndpoint(
        httpMethod = "test-method-3",
        path = "test-path-3",
        scopes = scopesB
      )

      val accessRequest = sampleAccessRequest().setEndpoints(Seq(endpoint1, endpoint2, endpoint3))

      val expected = Seq(
        AccessRequestEndpointGroup(0, scopesB.toSet, Seq(endpoint3)),
        AccessRequestEndpointGroup(1, scopesA.toSet, Seq(endpoint1, endpoint2))
      )

      val actual = AccessRequestEndpointGroups.group(accessRequest)

      actual mustBe expected
    }
  }

}
