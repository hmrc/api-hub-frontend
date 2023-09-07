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
import controllers.ApproveScopeControllerSpec.buildFixture
import controllers.actions.{FakeApprover, FakeUser}
import models.application._
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import views.html.{ApproveScopeView, ErrorTemplate}

import scala.concurrent.Future

class ApproveScopeControllerSpec extends SpecBase with MockitoSugar {

  "ApproveScopeController" - {
    "must approve the scope and redirect to the scope approved page when user is approver" in {
      val testId = "test-app-id"
      val scope = "my scope"
      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.approvePrimaryScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(scope))(any()))
        .thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onApprove(testId, scope).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ScopeApprovedController.onPageLoad().url)

        verify(fixture.apiHubService).approvePrimaryScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(scope))(any())
      }
    }

    "must block approve for users without approve scope" in {
      val fixture = buildFixture(FakeUser)
      val testId = "test-app-id"
      val scope = "my scope"
      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onApprove(testId, scope).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "must return OK and the correct view for a GET" in {
      val testId = "test-app-id"
      val fixture = buildFixture(FakeApprover)
      val view = fixture.application.injector.instanceOf[ApproveScopeView]
      val config = fixture.application.injector.instanceOf[FrontendAppConfig]

      val environmentsWithPrimaryPending = new Environments(
        new Environment(Seq(Scope("cheese", Pending)), Seq()),
        Environment()
      )

      val application = models.application.Application(testId, "app-name", Creator("test-creator-email"), environmentsWithPrimaryPending)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK

        contentAsString(result) mustEqual view(application, Some(FakeApprover), config.environmentNames)(request, messages(fixture.application)).toString
      }
    }

    "must block GET for users without approve scope" in {
      val testId = "test-app-id"
      val fixture = buildFixture(FakeUser)
      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "must return Not Found for a GET when the application does not exist" in {
      val testId = "test-app-id"
      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with Id $testId."
          )(request, messages(fixture.application))
            .toString()
      }
    }

    "must return Not Found while approving when the application does not exist" in {
      val testId = "test-app-id"
      val scope = "my scope"
      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.approvePrimaryScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(scope))(any()))
        .thenReturn(Future.successful(false))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onApprove(testId, scope).url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Scope request not found",
            s"Cannot find a request for scope $scope for an application with Id $testId."
          )(request, messages(fixture.application))
            .toString()
      }
    }
  }
}

object ApproveScopeControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService
  )

  def buildFixture(user: UserModel):Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = None, user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}
