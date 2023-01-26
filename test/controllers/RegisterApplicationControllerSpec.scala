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
import controllers.RegisterApplicationControllerSpec.buildFixture
import models.application.{Application, Creator, NewApplication}
import models.{CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import pages.ApplicationNamePage
import play.api.{Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService

import scala.concurrent.Future

class RegisterApplicationControllerSpec$ extends SpecBase with MockitoSugar {

  "CreateApplicationController" - {
    "must create the application and redirect to the Index page when valid" in {
      val newApplication = NewApplication("test-app-name", Creator(""))
      val testId = "test-app-id"
      val userAnswers = UserAnswers(userAnswersId)
        .set(ApplicationNamePage, newApplication.name)
        .get

      val fixture = buildFixture(userAnswers)

      when(fixture.apiHubService.registerApplication(ArgumentMatchers.eq(newApplication))(any()))
        .thenReturn(Future.successful(Application(testId, newApplication)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.RegisterApplicationController.create.url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CreateApplicationSuccessController.onPageLoad(testId).url)

        verify(fixture.apiHubService).registerApplication(ArgumentMatchers.eq(newApplication))(any())
      }
    }

    "must redirect to the journey recovery page when there is no application name user answer" in {
      val fixture = buildFixture(emptyUserAnswers)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.RegisterApplicationController.create.url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ApplicationNameController.onPageLoad(CheckMode).url)

        verifyZeroInteractions(fixture.apiHubService)
      }
    }
  }

}

object RegisterApplicationControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService
  )

  def buildFixture(userAnswers: UserAnswers): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}
