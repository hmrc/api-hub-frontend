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

import config.HipEnvironment
import controllers.actions.{FakeApiDetail, FakeApplication, FakePrivilegedUser, FakeUser}
import fakes.FakeHipEnvironments
import models.accessrequest.{AccessRequest, Pending}
import models.application.Credential
import models.user.UserModel
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.LocalDateTime

class EnvironmentsViewModelSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {
  "ApiTabViewModel" - {
    def buildApplicationEndpoint(prodAccess: ApplicationEndpointAccess) = {
      val approvedScopes = prodAccess match {
        case Accessible => Map(FakeHipEnvironments.production.id -> Seq("test-scope").toSet)
        case _ => Map.empty
      }
      ApplicationEndpoint(
        httpMethod = "GET",
        path = "/test1",
        summary = Some("test-summary"),
        description = Some("test-description"),
        scopes = Seq("test-scope"),
        theoreticalScopes = TheoreticalScopes(Seq("test-scope").toSet, approvedScopes),
        pendingAccessRequests = Seq.empty
      )
    }
    def buildViewModel(hipEnvironment: HipEnvironment, prodAccess: ApplicationEndpointAccess, pendingRequestCount: Int = 0) = {
      val applicationApis = Seq(ApplicationApi(FakeApiDetail, Seq(buildApplicationEndpoint(prodAccess)), buildAccessRequests(pendingRequestCount)))
      buildViewModelWithApplicationApis(applicationApis, hipEnvironment, prodAccess)
    }

    def buildViewModelWithApplicationApis(applicationApis: Seq[ApplicationApi], hipEnvironment: HipEnvironment, prodAccess: ApplicationEndpointAccess) = {
      EnvironmentsViewModel(FakeApplication, applicationApis, FakeUser, hipEnvironment, List.empty, "apiHubGuideUrl").apiTabViewModel
    }

    def buildAccessRequests(count: Int): Seq[AccessRequest] = {
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
          environmentId = FakeHipEnvironments.production.id
        )
      }
    }

    "showRequestProdAccessBanner" - {
      "must return true if the environment is production like and the application has an API that needs production access request" in {
        buildViewModel(FakeHipEnvironments.production, Inaccessible).showRequestProdAccessBanner mustBe true
      }
      "must return false if the environment is not production like" in {
        buildViewModel(FakeHipEnvironments.test, Inaccessible).showRequestProdAccessBanner mustBe false
      }
      "must return false if the application does not have an API that needs production access request" in {
        buildViewModel(FakeHipEnvironments.production, Accessible).showRequestProdAccessBanner mustBe false
      }
      "must return false if the application has a pending production access request" in {
        buildViewModel(FakeHipEnvironments.production, Inaccessible, 1).showRequestProdAccessBanner mustBe false
      }
    }

    "pendingAccessRequestsCount must return the number of APIs with pending production access requests" in {
      val applicationApis = Seq(
        ApplicationApi(FakeApiDetail, Seq(buildApplicationEndpoint(Accessible)), buildAccessRequests(1)),
        ApplicationApi(FakeApiDetail, Seq(buildApplicationEndpoint(Accessible)), buildAccessRequests(0)),
        ApplicationApi(FakeApiDetail, Seq(buildApplicationEndpoint(Accessible)), buildAccessRequests(3)),
        ApplicationApi(FakeApiDetail, Seq(buildApplicationEndpoint(Accessible)), buildAccessRequests(0)),
        ApplicationApi(FakeApiDetail, Seq(buildApplicationEndpoint(Accessible)), buildAccessRequests(1)),
      )
      buildViewModelWithApplicationApis(applicationApis, FakeHipEnvironments.production, Accessible).pendingAccessRequestsCount mustBe 3
    }

    "showPendingAccessRequestsBanner" - {
      "must return true if the environment is production like and there are pending production access requests" in {
        buildViewModel(FakeHipEnvironments.production, Inaccessible, 1).showPendingAccessRequestsBanner mustBe true
      }
      "must return false if the environment is not production like" in {
        buildViewModel(FakeHipEnvironments.test, Inaccessible, 1).showPendingAccessRequestsBanner mustBe false
      }
      "must return false if there are no pending production access requests" in {
        buildViewModel(FakeHipEnvironments.production, Inaccessible, 0).showPendingAccessRequestsBanner mustBe false
      }
    }
  }

  "EnvironmentsViewModel" - {
    def buildViewModel(credentialCount: Int, user: UserModel, hipEnvironment: HipEnvironment) = {
      val credentials = Range(0, credentialCount) map { index =>
        Credential(s"id$index", LocalDateTime.now(), None, None, hipEnvironment.id)
      }
      EnvironmentsViewModel(FakeApplication, Seq.empty, user, hipEnvironment, credentials, "apiHubGuideUrl").credentialsTabViewModel
    }
    "userCanAddCredentials" - {
      "must return true if the environment is not production like" in {
        buildViewModel(0, FakeUser, FakeHipEnvironments.test).userCanAddCredentials mustBe true
      }
      "must return true if the user is privileged" in {
        buildViewModel(0, FakePrivilegedUser, FakeHipEnvironments.production).userCanAddCredentials mustBe true
      }
      "must return false if the environment is production like and the user is not privileged" in {
        buildViewModel(0, FakeUser, FakeHipEnvironments.production).userCanAddCredentials mustBe false
      }
    }
    "maxCredentialsReached" - {
      "must return true if the number of credentials is equal to 5" in {
        buildViewModel(5, FakeUser, FakeHipEnvironments.test).maxCredentialsReached mustBe true
      }
      "must return false if the number of credentials is less than 5" in {
        buildViewModel(4, FakeUser, FakeHipEnvironments.test).maxCredentialsReached mustBe false
      }
    }
    "userCanDeleteCredentials" - {
      "must return true if the number of credentials is greater than 1 and the environment is not production like" in {
        buildViewModel(2, FakeUser, FakeHipEnvironments.test).userCanDeleteCredentials mustBe true
      }
      "must return true if the number of credentials is greater than 1 and the environment is production like the user is privileged" in {
        buildViewModel(2, FakePrivilegedUser, FakeHipEnvironments.production).userCanDeleteCredentials mustBe true
      }
      "must return false if the number of credentials is less than 2" in {
        buildViewModel(1, FakeUser, FakeHipEnvironments.test).userCanDeleteCredentials mustBe false
      }
      "must return false if the environment is production like and the user is not privileged" in {
        buildViewModel(2, FakeUser, FakeHipEnvironments.production).userCanDeleteCredentials mustBe false
      }
    }
    "addCredentialFormAction" - {
      "must return the correct checklist route if the environment is production like" in {
        buildViewModel(0, FakePrivilegedUser, FakeHipEnvironments.production).addCredentialFormAction mustBe controllers.application.routes.AddCredentialController.checklist(FakeApplication.id, FakeHipEnvironments.production.id)
      }
      "must return the correct addCredentialForEnvironment route if the environment is not production like" in {
        buildViewModel(0, FakeUser, FakeHipEnvironments.test).addCredentialFormAction mustBe controllers.application.routes.AddCredentialController.addCredentialForEnvironment(FakeApplication.id, FakeHipEnvironments.test.id)
      }
    }
    "showNoProductionCredentialsMessage" - {
      "must return true if the environment is production like and there are no credentials" in {
        buildViewModel(0, FakeUser, FakeHipEnvironments.production).showNoProductionCredentialsMessage mustBe true
      }
      "must return false if the environment is not production like" in {
        buildViewModel(0, FakeUser, FakeHipEnvironments.test).showNoProductionCredentialsMessage mustBe false
      }
      "must return false if the environment is production like and there are credentials" in {
        buildViewModel(1, FakeUser, FakeHipEnvironments.production).showNoProductionCredentialsMessage mustBe false
      }
    }
  }
}
