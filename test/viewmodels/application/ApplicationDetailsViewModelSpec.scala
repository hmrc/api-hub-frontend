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

package viewmodels.application

import controllers.actions.{FakeApplication, FakeUser}
import models.application.{Api, Application}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ApplicationDetailsViewModelSpec extends AnyFreeSpec with Matchers {
  "ApplicationDetailsViewModel" - {
    def buildApplicationApi(id: String, pendingCount: Int = 0, isMissing: Boolean = false) = {
      ApplicationApi(Api(id, id), pendingCount).copy(isMissing = isMissing)
    }
    def buildViewModel(application: Application = FakeApplication, applicationApis: Seq[ApplicationApi] = Seq.empty) = {
      ApplicationDetailsViewModel(application, applicationApis, Some(FakeUser))
    }

    "must return the correct application id" in {
      buildViewModel().applicationId mustBe FakeApplication.id
    }

    "must return the correct application name" in {
      buildViewModel().applicationName mustBe FakeApplication.name
    }

    "showApplicationProblemsPanel" - {
      "must show application problems panel when application has issues" in {
        buildViewModel(FakeApplication.copy(issues = Seq("big problem"))).showApplicationProblemsPanel mustBe true
      }
      "must not show application problems panel when application has issues" in {
        buildViewModel(FakeApplication.copy(issues = Seq.empty)).showApplicationProblemsPanel mustBe false
      }
    }

    "apiCount must match the number of APIs" in {
      buildViewModel(applicationApis = Seq(
        buildApplicationApi("a"), buildApplicationApi("b"), buildApplicationApi("c")
      )).apiCount mustBe 3
    }

    "noApis" - {
      "must be true when there are no APIs" in {
        buildViewModel().noApis mustBe true
      }
      "must be false when there are APIs" in {
        buildViewModel(applicationApis = Seq(buildApplicationApi("a"))).noApis mustBe false
      }
    }

    "missingApiNames" - {
      "must return the titles of missing APIs only" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a"), buildApplicationApi("b", isMissing = true), buildApplicationApi("c", isMissing = true)
        )).missingApiNames mustBe Seq("b", "c")
      }
    }

    "hasMissingApis" - {
      "must be true when there are missing APIs" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a"), buildApplicationApi("b", isMissing = true)
        )).hasMissingApis mustBe true
      }
      "must be false when there are no missing APIs" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a"), buildApplicationApi("b")
        )).hasMissingApis mustBe false
      }
    }

    "pendingAccessRequestsCount" - {
      "must return the total number of pending access requests" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a", 1), buildApplicationApi("b", 2), buildApplicationApi("c", 3)
        )).pendingAccessRequestsCount mustBe 6
      }
    }

    "hasPendingAccessRequests" - {
      "must be true when there are pending access requests" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a", 0), buildApplicationApi("b", 1), buildApplicationApi("c", 0)
        )).hasPendingAccessRequests mustBe true
      }
      "must be false when there are no pending access requests" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a", 0), buildApplicationApi("b", 0), buildApplicationApi("c", 0)
        )).hasPendingAccessRequests mustBe false
      }
    }
  }
}
