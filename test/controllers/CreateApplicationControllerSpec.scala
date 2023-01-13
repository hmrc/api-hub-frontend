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
import controllers.CreateApplicationControllerSpec.buildFixture
import models.{Application, CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import pages.ApplicationNamePage
import play.api.{Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApplicationsService

import scala.concurrent.Future

class CreateApplicationControllerSpec extends SpecBase with MockitoSugar {

  "CreateApplicationController" - {
    "must create the application and redirect to the Index page when valid" in {
      val application = Application(None, "test-app-name")

      val userAnswers = UserAnswers(userAnswersId)
        .set(ApplicationNamePage, application.name)
        .get

      val fixture = buildFixture(userAnswers)

      when(fixture.applicationsService.create(ArgumentMatchers.eq(application))(any()))
        .thenReturn(Future.successful(application.copy(id = Some("test-app-id"))))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.CreateApplicationController.create.url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IndexController.onPageLoad.url)

        verify(fixture.applicationsService).create(ArgumentMatchers.eq(application))(any())
      }
    }

    "must redirect to the application name page when there is no user answer" in {
      val fixture = buildFixture(emptyUserAnswers)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.CreateApplicationController.create.url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ApplicationNameController.onPageLoad(CheckMode).url)

        verifyZeroInteractions(fixture.applicationsService)
      }
    }
  }

}

object CreateApplicationControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    applicationsService: ApplicationsService
  )

  def buildFixture(userAnswers: UserAnswers): Fixture = {
    val applicationsService = mock[ApplicationsService]

    val application = applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[ApplicationsService].toInstance(applicationsService)
      )
      .build()

    Fixture(application, applicationsService)
  }

}
