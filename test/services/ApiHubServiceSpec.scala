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

package services

import connectors.ApplicationsConnector
import models.application._
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.OptionValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ApiHubServiceSpec extends AsyncFreeSpec with Matchers with MockitoSugar with OptionValues {

  "registerApplication" - {
    "must call the applications connector and return the saved application" in {
      val newApplication = NewApplication("test-app-name", Creator("test-creator-email"), Seq(TeamMember("test-creator-email")))
      val expected = Application("id", newApplication)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.registerApplication(ArgumentMatchers.eq(newApplication))(any()))
        .thenReturn(Future.successful(expected))

      val service = new ApiHubService(applicationsConnector)

      service.registerApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).registerApplication(ArgumentMatchers.eq(newApplication))(any())
          succeed
      }
    }
  }

  "getApplications" - {
    "must call the applications connector and return a sequence of applications" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.getApplications()(any())).thenReturn(Future.successful(expected))

      val service = new ApiHubService(applicationsConnector)

      service.getApplications()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getApplications()(any())
          succeed
      }
    }
    "must call the applications connector and return a sequence of applications with a given team member" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.getUserApplications(ArgumentMatchers.eq("test-creator-email-2"))(any())).thenReturn(Future.successful(expected))

      val service = new ApiHubService(applicationsConnector)

      service.getUserApplications("test-creator-email-2")(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getUserApplications(ArgumentMatchers.eq("test-creator-email-2"))(any())
          succeed
      }
  }}

  "getApplication" - {
    "must call the applications connector and return an application" in {
      val application = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val expected = Some(application)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.getApplication(ArgumentMatchers.eq("id-1"))(any())).thenReturn(Future.successful(expected))

      val service = new ApiHubService(applicationsConnector)

      service.getApplication("id-1")(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).getApplication(ArgumentMatchers.eq("id-1"))(any())
          succeed
      }
    }
  }

  "deleteApplication" - {
    "must delete the application via the applications connector" in {
      val id = "test-id"

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.deleteApplication(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(Some(())))

      val service = new ApiHubService(applicationsConnector)

      service.deleteApplication(id)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
          verify(applicationsConnector).deleteApplication(any())(any())
          succeed
      }
    }

    "must return None when the applications connectors does to indicate the application was not found" in {
      val id = "test-id"

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.deleteApplication(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(None))

      val service = new ApiHubService(applicationsConnector)

      service.deleteApplication(id)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
          succeed
      }
    }
  }

  "requestAdditionalScope" - {
    "must call the applications connector and return the new scope" in {
      val applicationId = "app-id"
      val newScope = NewScope(applicationId, Seq(Primary))

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.requestAdditionalScope(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(newScope))(any()))
        .thenReturn(Future.successful(Some(newScope)))

      val service = new ApiHubService(applicationsConnector)

      service.requestAdditionalScope(applicationId, newScope)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(newScope)
          verify(applicationsConnector).requestAdditionalScope(ArgumentMatchers.eq(applicationId), ArgumentMatchers.eq(newScope))(any())
          succeed
      }
    }
  }

  "pendingScopes" - {
    "must call the applications connectors and return the applications" in {
      val application1 = Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.pendingPrimaryScopes()(any())).thenReturn(Future.successful(expected))

      val service = new ApiHubService(applicationsConnector)

      service.pendingPrimaryScopes()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).pendingPrimaryScopes()(any())
          succeed
      }
    }
  }

  "approveScope" - {
    "must call the applications connectors and return APPROVED" in {
      val appId = "app_id"
      val scope = "a_scope"
      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.approvePrimaryScope(ArgumentMatchers.eq(appId),ArgumentMatchers.eq(scope))(any()))
        .thenReturn(Future.successful(true))

      val service = new ApiHubService(applicationsConnector)

      service.approvePrimaryScope(appId,scope)(HeaderCarrier()) map {
        actual =>
          actual mustBe true
          verify(applicationsConnector).approvePrimaryScope(ArgumentMatchers.eq(appId),ArgumentMatchers.eq(scope))(any())
          succeed
      }
    }
  }

  "createPrimarySecret" - {
    "must call the applications connector and return the secret" in {
      val applicationId = "test-application-id"
      val expected = Secret("test-secret")
      val applicationsConnector = mock[ApplicationsConnector]
      val service = new ApiHubService(applicationsConnector)

      when(applicationsConnector.createPrimarySecret(ArgumentMatchers.eq(applicationId))(any()))
        .thenReturn(Future.successful(Some(expected)))

      service.createPrimarySecret(applicationId)(HeaderCarrier()) map {
        actual =>
          actual.value mustBe expected
      }
    }
  }

}
