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
import controllers.ApproveScopeControllerSpec.buildFixture
import controllers.actions.{FakeApprover, FakeUser}
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
import views.html.ApproveProductionScopeView

import scala.concurrent.Future

class ApproveScopeControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new NewScopeFormProvider()
  val form = formProvider()

  "ApproveScopeController" - {
    "must approve the scope and redirect to the pending applications page when user is approver" in {
      val testId = "test-app-id"
      val scope = "my scope"
      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.approveProductionScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(scope))(any()))
        .thenReturn(Future.successful(Some("APPROVED")))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onApprove(testId, scope).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PendingApprovalsController.onPageLoad().url)

        verify(fixture.apiHubService).approveProductionScope(ArgumentMatchers.eq(testId), ArgumentMatchers.eq(scope))(any())
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
      val view = fixture.application.injector.instanceOf[ApproveProductionScopeView]

      val environmentsWithProdPending = new Environments(
        Environment(),
        Environment(),
        Environment(),
        new Environment(Seq(Scope("cheese", Pending)), Seq())
      )

      val application = models.application.Application(testId, "app-name", Creator("test-creator-email"), environmentsWithProdPending)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId))(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApproveScopeController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK

        contentAsString(result) mustEqual view(application, Some(FakeApprover))(request, messages(fixture.application)).toString
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
