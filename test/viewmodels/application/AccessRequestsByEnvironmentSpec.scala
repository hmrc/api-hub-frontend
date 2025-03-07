/*
 * Copyright 2025 HM Revenue & Customs
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

import fakes.FakeHipEnvironments
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import generators.AccessRequestGenerator

class AccessRequestsByEnvironmentSpec  extends AnyFreeSpec with Matchers with AccessRequestGenerator {
  "AccessRequestsByEnvironmentSpec" - {
    "must group access requests correctly" in {
      val request1 = sampleAccessRequest().copy(environmentId = FakeHipEnvironments.production.id)
      val request2 = sampleAccessRequest().copy(environmentId = FakeHipEnvironments.preProduction.id)
      val request3 = sampleAccessRequest().copy(environmentId = FakeHipEnvironments.production.id)
      val request4 = sampleAccessRequest().copy(environmentId = FakeHipEnvironments.preProduction.id)
      
      val accessRequests = Seq(request1, request2, request3, request4)
      
      val result = AccessRequestsByEnvironment(accessRequests, FakeHipEnvironments).groupByEnvironment
      result mustBe Seq(
        EnvironmentAccessRequests(FakeHipEnvironments.preProduction, Set(request2, request4)),
        EnvironmentAccessRequests(FakeHipEnvironments.production, Set(request1, request3))
      )
    }
  }
}
