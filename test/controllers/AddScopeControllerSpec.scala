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
import config.FrontendAppConfig
import controllers.AddScopeControllerSpec.buildFixture
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import forms.NewScopeFormProvider
import models.application._
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.AddScopeView

import scala.concurrent.Future

class AddScopeControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  private val formProvider = new NewScopeFormProvider()
  private val form = formProvider()

  "AddScopeController" - {
    "must register the scope and redirect to the application details page for a valid request from a team member or supporter" in {
      val testId = "test-app-id"
      val newScope = NewScope("my_scope", Seq(Primary, Secondary))

      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
            .thenReturn(Future.successful(Some(FakeApplication)))

          when(fixture.apiHubService.requestAdditionalScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(newScope))(any()))
            .thenReturn(Future.successful(Some(newScope)))

          running(fixture.application) {
            val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url)
              .withFormUrlEncodedBody(("scope-name","my_scope"),("primary","primary"),("secondary","secondary"))
            val result = route(fixture.application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.RequestScopeSuccessController.onPageLoad(testId).url)

            verify(fixture.apiHubService).requestAdditionalScope(ArgumentMatchers.eq(testId),ArgumentMatchers.eq(newScope))(any())
          }
      }
    }

    "must redirect to Unauthorised page for a POST when user is not a team member or supporter" in {
      val testId = "test-app-id"
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url)
          .withFormUrlEncodedBody(("scope-name", "my_scope"), ("primary", "primary"), ("secondary", "secondary"))
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER

        val actualRedirectLocation = redirectLocation(result).value
        val expectedRedirectLocation = routes.UnauthorisedController.onPageLoad.url

        actualRedirectLocation mustEqual expectedRedirectLocation
      }
    }

    "must show same page with errors when there is no scope name or environments" in {
      val testId = "test-app-id"
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Error: Add a Scope - The API Hub - GOV.UK")
        contentAsString(result) must validateAsHtml

        verifyZeroInteractions(fixture.apiHubService.requestAdditionalScope(any(), any())(any()))
      }
    }

    "must show same page with errors when there is no scope name and at least one environment" in {
      val testId = "test-app-id"
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url).withFormUrlEncodedBody(("dev","dev"))
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Error: Add a Scope - The API Hub - GOV.UK")
        contentAsString(result) must validateAsHtml

        verifyZeroInteractions(fixture.apiHubService.requestAdditionalScope(any(), any())(any()))
      }
    }

    "must show same page with errors when there is a scope name but no environment" in {
      val testId = "test-app-id"
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url).withFormUrlEncodedBody(("scope-name", "a scope"))
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Error: Add a Scope - The API Hub - GOV.UK")
        contentAsString(result) must validateAsHtml

        verifyZeroInteractions(fixture.apiHubService.requestAdditionalScope(any(), any())(any()))
      }
    }

    "must return OK and the correct view for a GET for a team member or supporter" in {
      val testId = "test-app-id"
      val env1 = "Narnia"
      val env2 = "Coventry"

      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val fixture = buildFixture(env1, env2, user)
          val view = fixture.application.injector.instanceOf[AddScopeView]
          val config = fixture.application.injector.instanceOf[FrontendAppConfig]
          val application = models.application.Application(testId, "app-name", Creator("test-creator-email"), Seq(TeamMember("test-email")))

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
            .thenReturn(Future.successful(Some(application)))

          running(fixture.application) {
            val request = FakeRequest(GET, routes.AddScopeController.onPageLoad(testId).url)
            val result = route(fixture.application, request).value

            status(result) mustBe OK

            val content = contentAsString(result)
            content mustEqual view(testId, form, Some(user), config)(request, messages(fixture.application)).toString
            content must include(env1)
            content must include(env2)
            content must validateAsHtml
          }
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member" in {
      val testId = "test-app-id"
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddScopeController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER

        val actualRedirectLocation = redirectLocation(result).value
        val expectedRedirectLocation = routes.UnauthorisedController.onPageLoad.url

        actualRedirectLocation mustEqual expectedRedirectLocation
      }
    }

  }

}

object AddScopeControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService
  )

  def buildFixture(environment1: String = "primary", environment2: String = "secondary", userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = None, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .configure("environment-names.primary" -> environment1, "environment-names.secondary" -> environment2)
      .build()

    Fixture(application, apiHubService)
  }

}
