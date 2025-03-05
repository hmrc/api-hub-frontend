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
import fakes.FakeHipEnvironments
import models.accessrequest.{AccessRequest, Pending}
import models.application.{Api, Application, TeamMember}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDateTime

class ApplicationDetailsViewModelSpec extends AnyFreeSpec with Matchers {
  "ApplicationDetailsViewModel" - {
    def buildApplicationApi(id: String, pendingCount: Int = 0, isMissing: Boolean = false) = {
      ApplicationApi(Api(id, id), buildPendingAccessRequests(pendingCount)).copy(isMissing = isMissing)
    }
    def buildApplicationEndpoint() = {
      ApplicationEndpoint("GET", "/path", None, None, Seq("scope"), TheoreticalScopes(Seq("scope").toSet, Map(
        FakeHipEnvironments.production.id -> Seq("scope").toSet,
        FakeHipEnvironments.preProduction.id -> Seq("scope").toSet,
      )), Seq.empty)
    }
    def buildApplicationApiWithEndpoint(id: String, endpoint: ApplicationEndpoint) = {
      buildApplicationApi(id).copy(endpoints = Seq(endpoint))
    }
    def buildViewModel(application: Application = FakeApplication, applicationApis: Seq[ApplicationApi] = Seq.empty) = {
      ApplicationDetailsViewModel(application, applicationApis, Some(FakeUser), FakeHipEnvironments)
    }

    def buildPendingAccessRequests(count: Int): Seq[AccessRequest] = {
      Range(0, count) map { index =>
        AccessRequest(
          id = s"id$index",
          applicationId = "applicationId",
          apiId = "apiId",
          apiName = "apiName",
          status = Pending,
          supportingInformation = "supportingInformation",
          requested = LocalDateTime.now(),
          requestedBy = "requestedBy",
          environmentId = "environmentId"
        )
      }
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

    "allApiNames" - {
      "must return the titles of all APIs" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApi("a"), buildApplicationApi("b"), buildApplicationApi("c")
        )).allApiNames mustBe Seq("a", "b", "c")
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

    "needsProductionAccessRequest" - {
      "must be true when at least one API needs a production access request" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApiWithEndpoint("1", buildApplicationEndpoint()),
          buildApplicationApiWithEndpoint("2", buildApplicationEndpoint().copy(theoreticalScopes = TheoreticalScopes(Seq("scope").toSet, Map.empty))),
          buildApplicationApiWithEndpoint("3", buildApplicationEndpoint()),
        )).needsProductionAccessRequest mustBe true
      }
      "must be false when no APIs need a production access request" in {
        buildViewModel(applicationApis = Seq(
          buildApplicationApiWithEndpoint("1", buildApplicationEndpoint()),
          buildApplicationApiWithEndpoint("2", buildApplicationEndpoint()),
          buildApplicationApiWithEndpoint("3", buildApplicationEndpoint()),
        )).needsProductionAccessRequest mustBe false
      }
    }

    "notUsingGlobalTeams" - {
      "must be true when there is no application team" in {
        buildViewModel(FakeApplication.copy(teamId = None)).notUsingGlobalTeams mustBe true
      }
      "must be false when there is an application team" in {
        buildViewModel(FakeApplication.copy(teamId = Some("team"))).notUsingGlobalTeams mustBe false
      }
    }

    "applicationTeamMemberCount" - {
      "must return the number of team members" in {
        buildViewModel(FakeApplication.copy(teamMembers = Seq(TeamMember("a@example.com"), TeamMember("b@example.com")))).applicationTeamMemberCount mustBe 2
      }
    }

    "applicationTeamMemberEmails" - {
      "must return the emails of team members" in {
        buildViewModel(FakeApplication.copy(teamMembers = Seq(TeamMember("a@example.com"), TeamMember("b@example.com")))).applicationTeamMemberEmails mustBe Seq("a@example.com", "b@example.com")
      }
    }

  }
}
