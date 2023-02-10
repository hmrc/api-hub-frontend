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
import controllers.actions.FakeUser
import models.application.Application
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
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .bindings(
          bind[ApiHubService].toInstance(apiHubService)
        )
        .build()

      val applications: Seq[Application] = Seq.empty
      when(apiHubService.pendingScopes()(any())).thenReturn(Future.successful(applications))

      running(application) {
        val request = FakeRequest(GET, routes.PendingApprovalsController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PendingApprovalsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(applications, Some(FakeUser))(request, messages(application)).toString
      }
    }
  }

}
