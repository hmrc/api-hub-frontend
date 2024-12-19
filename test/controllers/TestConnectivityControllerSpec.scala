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
import controllers.TestConnectivityControllerSpec.buildFixture
import controllers.actions.FakeUser
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.TestConnectivityView

import scala.concurrent.Future

class TestConnectivityControllerSpec extends SpecBase with MockitoSugar with HtmlValidation {

  "Test Connectivity Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture()
      val expected: String = "something"
      running(fixture.application) {

        when(fixture.mockApiHubService.testConnectivity()(any()))
          .thenReturn(Future.successful(expected))

        val request = IdentifierRequest(FakeRequest(GET, routes.TestConnectivityController.onPageLoad().url), FakeUser)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[TestConnectivityView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(expected)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }
}

object TestConnectivityControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    mockSessionRepository: SessionRepository,
    mockApiHubService: ApiHubService
  )

  def buildFixture(): Fixture = {
    val mockSessionRepository = mock[SessionRepository]
    val mockApiHubService = mock[ApiHubService]
    val application =
      applicationBuilder(userAnswers = None)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ApiHubService].toInstance(mockApiHubService)
        )
        .build()
    Fixture(application, mockSessionRepository, mockApiHubService)
  }
}
