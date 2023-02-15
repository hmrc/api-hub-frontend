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

package controllers

import base.SpecBase
import controllers.actions.{FakeApprover, FakeUser}
import models.application.{Application, Creator}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.PendingApprovalsView

import scala.concurrent.Future

class PendingApprovalsControllerSpec extends SpecBase with MockitoSugar {

  "PendingApprovals Controller" - {

    "must return OK and the correct view for a GET" in {

      val apiHubService = mock[ApiHubService]
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = FakeApprover)
        .bindings(
          bind[ApiHubService].toInstance(apiHubService)
        )
        .build()

      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"))
      val applications: Seq[Application] = Seq(application1, application2)

      when(apiHubService.pendingScopes()(any())).thenReturn(Future.successful(applications))

      running(application) {
        val request = FakeRequest(GET, routes.PendingApprovalsController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PendingApprovalsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(applications, Some(FakeApprover))(request, messages(application)).toString
      }
    }

    "must redirect to the unauthorised page when the user is not an approver" in {
      val apiHubService = mock[ApiHubService]
      val application = applicationBuilder(user = FakeUser)
        .bindings(
          bind[ApiHubService].toInstance(apiHubService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PendingApprovalsController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

}
