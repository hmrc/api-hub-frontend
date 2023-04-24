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
import controllers.GeneratePrimarySecretSuccessControllerSpec.buildFixture
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Approved, Credential, Scope, Secret}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.{Configuration, Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import viewmodels.GeneratePrimarySecretSuccessViewModel
import views.html.GeneratePrimarySecretSuccessView

import scala.concurrent.Future

class GeneratePrimarySecretSuccessControllerSpec extends SpecBase with MockitoSugar {

  "GeneratePrimarySecretSuccess Controller" - {

    "must return OK and the correct view for a GET" in {
      val credential = Credential("test-client-id", None, None)
      val application = FakeApplication
        .setPrimaryScopes(Seq(Scope("test-scope-1", Approved), Scope("test-scope-2", Approved)))
        .setPrimaryCredentials(Seq(credential))
      val secret = Secret("test-secret")

      val fixture = buildFixture()

      running(fixture.playApplication) {
        when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id))(any()))
          .thenReturn(Future.successful(Some(application)))

        when(fixture.apiHubService.createPrimarySecret(ArgumentMatchers.eq(application.id))(any()))
          .thenReturn(Future.successful(Some(secret)))

        val request = FakeRequest(GET, routes.GeneratePrimarySecretSuccessController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[GeneratePrimarySecretSuccessView]
        val viewModel = GeneratePrimarySecretSuccessViewModel
          .buildSummary(
            application,
            fixture.playApplication.injector.instanceOf[FrontendAppConfig].environmentNames,
            credential,
            secret
          )(messages(fixture.playApplication))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(application, viewModel, Some(FakeUser))(request, messages(fixture.playApplication)).toString
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      running(fixture.playApplication) {
        when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id))(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.GeneratePrimarySecretSuccessController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "must return 400 Bad Request when the application does not have a valid primary credential" in {
      val credential = Credential("test-client-id", None, Some("secret-fragment"))
      val application = FakeApplication
        .setPrimaryCredentials(Seq(credential))

      val fixture = buildFixture()

      running(fixture.playApplication) {
        when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id))(any()))
          .thenReturn(Future.successful(Some(application)))

        val request = FakeRequest(GET, routes.GeneratePrimarySecretSuccessController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      running(fixture.playApplication) {
        when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id))(any()))
          .thenReturn(Future.successful(Some(FakeApplication)))

        val request = FakeRequest(GET, routes.GeneratePrimarySecretSuccessController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

  }

}

object GeneratePrimarySecretSuccessControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val primaryEnvName = "primary-env-name"
    val secondaryEnvName = "secondary-env-name"

    val configuration = Configuration.from(Map(
      "environment-names.primary" -> primaryEnvName,
      "environment-names.secondary" -> secondaryEnvName
    ))

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel, configuration)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
