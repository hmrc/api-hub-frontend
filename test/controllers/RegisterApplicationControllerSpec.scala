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
import controllers.actions.FakeUser
import models.application.{Application, Creator, NewApplication, TeamMember}
import models.{CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import pages.{ApplicationNamePage, TeamMembersPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.SessionRepository
import services.ApiHubService

import scala.concurrent.Future

class RegisterApplicationControllerSpec extends SpecBase with MockitoSugar {

  "RegisterApplicationController" - {
    "must register the application and redirect to the Index page when valid" in {
      val teamMembers = Seq(TeamMember(FakeUser.email.value), TeamMember("team.member@hmrc.gov.uk"))
      val newApplication = NewApplication("test-app-name", Creator(FakeUser.email.value), teamMembers)
      val testId = "test-app-id"
      val userAnswers = UserAnswers(FakeUser.userId)
        .set(ApplicationNamePage, newApplication.name)
        .success
        .value
        .set(TeamMembersPage, teamMembers)
        .success
        .value

      val fixture = buildFixture(userAnswers)

      when(fixture.apiHubService.registerApplication(ArgumentMatchers.eq(newApplication))(any()))
        .thenReturn(Future.successful(Application(testId, newApplication)))

      when(fixture.sessionRepository.clear(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.RegisterApplicationController.create.url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegisterApplicationSuccessController.onPageLoad(testId).url)

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

    "must clear UserAnswers after registration" in {
      val newApplication = NewApplication("test-app-name", Creator(FakeUser.email.get))
      val testId = "test-app-id"
      val userAnswers = UserAnswers(FakeUser.userId)
        .set(ApplicationNamePage, newApplication.name)
        .get

      val fixture = buildFixture(userAnswers)

      when(fixture.apiHubService.registerApplication(any())(any()))
        .thenReturn(Future.successful(Application(testId, newApplication)))

      when(fixture.sessionRepository.clear(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.RegisterApplicationController.create.url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        verify(fixture.sessionRepository).clear(userAnswersId)
      }
    }
  }

}

object RegisterApplicationControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService,
    sessionRepository: SessionRepository
  )

  def buildFixture(userAnswers: UserAnswers): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[SessionRepository]

    val application = applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

    Fixture(application, apiHubService, sessionRepository)
  }

}
