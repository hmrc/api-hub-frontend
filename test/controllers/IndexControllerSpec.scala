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
import controllers.IndexControllerSpec.{buildFixture, buildFixtureWithUser}
import controllers.actions.FakeUser
import models.application.{Application, Creator, TeamMember}
import models.user.UserModel
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers, MockitoSugar}
import pages.TeamMembersPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.{ErrorTemplate, IndexView}

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with MockitoSugar with HtmlValidation {

  "Index Controller" - {

    "must return OK and the correct view for a GET" in {

      val testEmail = "test-email"
      val creatorEmail = "creator-email-2"
      val applications = Seq(
        Application("id-1", "app-name-1", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail))),
        Application("id-2", "app-name-2", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail)))
      )

      val fixture = buildFixture()

      running(fixture.application) {

        when(fixture.mockApiHubService.getApplications(ArgumentMatchers.eq(Some(testEmail)), ArgumentMatchers.eq(false))(any()))
          .thenReturn(Future.successful(applications))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[IndexView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(applications, applications.size, Some(FakeUser))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must initiate User Answers and redirect to the Application Name page for a POST" in {
      val fixture = buildFixture()

      when(fixture.mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.IndexController.onSubmit.url)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ApplicationNameController.onPageLoad(NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(fixture.mockSessionRepository).set(userAnswersCaptor.capture())

        val actualUserAnswers = userAnswersCaptor.getValue
        actualUserAnswers.id mustBe userAnswersId
        actualUserAnswers.get(TeamMembersPage) mustBe Some(Seq(TeamMember(FakeUser.email.value)))
      }
    }

    "must initiate User Answers and redirect to the Application Name page for a GET" in {
      val fixture = buildFixture()

      when(fixture.mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(fixture.application) {
        val request = FakeRequest(GET, routes.IndexController.createApplication.url)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ApplicationNameController.onPageLoad(NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(fixture.mockSessionRepository).set(userAnswersCaptor.capture())

        val actualUserAnswers = userAnswersCaptor.getValue
        actualUserAnswers.id mustBe userAnswersId
        actualUserAnswers.get(TeamMembersPage) mustBe Some(Seq(TeamMember(FakeUser.email.value)))
      }
    }

    "must call apiHubService.getUserApplications()" in {
      val testEmail = "test-email"
      val creatorEmail = "creator-email-2"
      val applications = Seq(
        Application("id-1", "app-name-1", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail))),
        Application("id-2", "app-name-2", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail)))
      )
      val fixture = buildFixture()
      running(fixture.application) {

        when(fixture.mockApiHubService.getApplications(ArgumentMatchers.eq(Some(testEmail)), ArgumentMatchers.eq(false))(any()))
          .thenReturn(Future.successful(applications))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[IndexView]
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(applications, applications.size, Some(FakeUser))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return Internal Server Error if the user has no email address for a GET" in {
      val fixture = buildFixtureWithUser(FakeUser.copy(email = None))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe
          view(
            "Sorry, we are experiencing technical difficulties - 500",
            "Sorry, we’re experiencing technical difficulties",
            "Please try again in a few minutes."
          )(request, messages(fixture.application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return Internal Server Error if the user has no email address while attempting to create an application" in {
      val fixture = buildFixtureWithUser(FakeUser.copy(email = None))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.IndexController.onSubmit.url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe
          view(
            "Sorry, we are experiencing technical difficulties - 500",
            "Sorry, we’re experiencing technical difficulties",
            "Please try again in a few minutes."
          )(request, messages(fixture.application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

}

object IndexControllerSpec extends SpecBase with MockitoSugar {

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

  def buildFixtureWithUser(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]
    val mockSessionRepository = mock[SessionRepository]
    val application = applicationBuilder(userAnswers = None, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, mockSessionRepository, apiHubService)
  }

}
