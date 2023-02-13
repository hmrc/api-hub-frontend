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
import controllers.AddScopeControllerSpec.buildFixture
import controllers.actions.FakeUser
import forms.NewScopeFormProvider
import models.application._
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import views.html.AddScopeView

import scala.concurrent.Future

class AddScopeControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new NewScopeFormProvider()
  val form = formProvider()

  "AddScopeController" - {
    "must register the scope and redirect to the application details page when valid" in {
      val testId = "test-app-id"
      val newScope = NewScope("my_scope", Seq(Dev,Test,PreProd,Prod))
      val fixture = buildFixture()

      when(fixture.apiHubService.requestAdditionalScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(newScope))(any()))
        .thenReturn(Future.successful(Some(newScope)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url)
          .withFormUrlEncodedBody(("scope-name","my_scope"),("dev","dev"),("test","test"),("preProd","preProd"),("prod","prod"))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RequestScopeSuccessController.onPageLoad(testId).url)

        verify(fixture.apiHubService).requestAdditionalScope(ArgumentMatchers.eq(testId),ArgumentMatchers.eq(newScope))(any())
      }
    }

    "must show same page with errors when there is no scope name or environments" in {
      val testId = "test-app-id"
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Error: Add a Scope - The API Hub - GOV.UK")

        verifyZeroInteractions(fixture.apiHubService)
      }
    }

    "must show same page with errors when there is no scope name and at least one environment" in {
      val testId = "test-app-id"
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url).withFormUrlEncodedBody(("dev","dev"))
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Error: Add a Scope - The API Hub - GOV.UK")

        verifyZeroInteractions(fixture.apiHubService)
      }
    }

    "must show same page with errors when there is a scope name but no environment" in {
      val testId = "test-app-id"
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddScopeController.onSubmit(testId).url).withFormUrlEncodedBody(("scope-name", "a scope"))
        val result = route(fixture.application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Error: Add a Scope - The API Hub - GOV.UK")

        verifyZeroInteractions(fixture.apiHubService)
      }
    }

    "must return OK and the correct view for a GET" in {
      val testId = "test-app-id"
      val fixture = buildFixture()
      val view = fixture.application.injector.instanceOf[AddScopeView]

      val application = models.application.Application(testId, "app-name", Creator("test-creator-email"))

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId))(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddScopeController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK

        contentAsString(result) mustEqual view(testId, form, Some(FakeUser))(request, messages(fixture.application)).toString
      }
    }
  }
}

object AddScopeControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService
  )

  def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = None)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}
