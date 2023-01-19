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
import models.application.{Application, NewApplication}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ApiHubServiceSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  "createApplication" - {
    "must call the applications connector and return the saved application" in {
      val newApplication = NewApplication("test-app-name")
      val expected = Application(newApplication).copy(id = Some("id"))

      val applicationsConnector = mock[ApplicationsConnector]
      when(applicationsConnector.createApplication(ArgumentMatchers.eq(newApplication))(any()))
        .thenReturn(Future.successful(expected))

      val service = new ApiHubService(applicationsConnector)

      service.createApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
          verify(applicationsConnector).createApplication(ArgumentMatchers.eq(newApplication))(any())
          succeed
      }
    }
  }

  "getApplications" - {
    "must call the applications connector and return a sequence of applications" in {
      val application1 = Application(Some("id-1"), "test-app-name-1")
      val application2 = Application(Some("id-2"), "test-app-name-2")
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
  }

}
