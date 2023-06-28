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
import config.EnvironmentNames
import controllers.ApplicationDetailsControllerSpec.buildFixture
import controllers.actions._
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Application => PlayApplication}
import services.ApiHubService
import utils.TestHelpers
import views.html.ApplicationDetailsView

import scala.concurrent.Future

class ApplicationDetailsControllerSpec extends SpecBase with MockitoSugar with TestHelpers {

  "ApplicationDetails Controller" - {

    "must return OK and the correct view for a GET for a team member or administrator" in {
      val primaryEnvName = "primary-env-name"
      val secondaryEnvName = "secondary-env-name"

      val configWithEnvironmentNames = Configuration.from(Map(
        "environment-names.primary" -> primaryEnvName,
        "environment-names.secondary" -> secondaryEnvName
      ))

      val idWithSecrets = "test-id-with-secrets"

      forAll(teamMemberAndAdministratorTable) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user, testConfiguration = configWithEnvironmentNames)

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(idWithSecrets), ArgumentMatchers.eq(true))(any()))
            .thenReturn(Future.successful(Some(FakeApplicationWithSecrets)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, routes.ApplicationDetailsController.onPageLoad(idWithSecrets).url)

            val result = route(fixture.playApplication, request).value

            val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]

            status(result) mustEqual OK

            val content = contentAsString(result)
            content mustEqual view(
              FakeApplicationWithSecrets, Some(user), EnvironmentNames(primaryEnvName, secondaryEnvName)
            )(request, messages(fixture.playApplication)).toString

            content must include (messages(fixture.playApplication).apply("applicationDetails.credentials.clientSecret"))
            content must include (messages(fixture.playApplication).apply("applicationDetails.credentials.clientId"))
            content must include("secondary secret")
            content must include("secondary_client_id")
          }
      }
    }

    "must return OK and the correct view for a GET when no application secrets exist" in {
      val primaryEnvName = "primary-env-name"
      val secondaryEnvName = "secondary-env-name"

      val configWithEnvironmentNames = Configuration.from(Map(
        "environment-names.primary" -> primaryEnvName,
        "environment-names.secondary" -> secondaryEnvName
      ))

      val fixture = buildFixture(testConfiguration = configWithEnvironmentNames)

      val idNoSecrets = "test-id-no-secrets"
      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(idNoSecrets), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Some(FakeApplicationWithIdButNoSecrets)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.ApplicationDetailsController.onPageLoad(idNoSecrets).url)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]

        status(result) mustEqual OK

        val content = contentAsString(result)
        content mustEqual view(
          FakeApplicationWithIdButNoSecrets, Some(FakeUser), EnvironmentNames(primaryEnvName, secondaryEnvName)
        )(request, messages(fixture.playApplication)).toString

        content must include(messages(fixture.playApplication).apply("applicationDetails.generateSecret"))
      }
    }
  }

  "must return 404 Not Found when the application does not exist" in {
    val fixture = buildFixture()

    val id = "test-id"

    when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(id), ArgumentMatchers.eq(true))(any()))
      .thenReturn(Future.successful(None))

    running(fixture.playApplication) {
      val request = FakeRequest(GET, routes.ApplicationDetailsController.onPageLoad(id).url)

      val result = route(fixture.playApplication, request).value
      status(result) mustBe NOT_FOUND
    }
  }

  "must redirect to Unauthorised page for a GET when user is not a team member or administrator" in {
    val fixture = buildFixture(userModel = FakeUserNotTeamMember)

    val id = "test-id"
    when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(id), ArgumentMatchers.eq(true))(any()))
      .thenReturn(Future.successful(Some(FakeApplication)))

    running(fixture.playApplication) {
      val request = FakeRequest(GET, routes.ApplicationDetailsController.onPageLoad(id).url)

      val result = route(fixture.playApplication, request).value

      status(result) mustEqual SEE_OTHER

      val actualRedirectLocation = redirectLocation(result).value
      val expectedRedirectLocation = routes.UnauthorisedController.onPageLoad.url

      actualRedirectLocation mustEqual expectedRedirectLocation
    }
  }

  "must delete the application and redirect to the landing page for a DELETE for a team member or administrator" in {
    forAll(teamMemberAndAdministratorTable) {
      user: UserModel =>
        val fixture = buildFixture(userModel = user)

        when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(false))(any()))
          .thenReturn(Future.successful(Some(FakeApplication)))

        when(fixture.apiHubService.deleteApplication(ArgumentMatchers.eq(FakeApplication.id))(any()))
          .thenReturn(Future.successful(Some(())))

        running(fixture.playApplication) {
          val request = FakeRequest(POST, routes.ApplicationDetailsController.delete(FakeApplication.id).url)

          val result = route(fixture.playApplication, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.IndexController.onPageLoad.url
        }
    }
  }

  "must return NotFound for a DELETE when the application is not found" in {
    val fixture = buildFixture()

    when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(false))(any()))
      .thenReturn(Future.successful(Some(FakeApplication)))

    when(fixture.apiHubService.deleteApplication(ArgumentMatchers.eq(FakeApplication.id))(any()))
      .thenReturn(Future.successful(None))

    running(fixture.playApplication) {
      val request = FakeRequest(POST, routes.ApplicationDetailsController.delete(FakeApplication.id).url)

      val result = route(fixture.playApplication, request).value

      status(result) mustEqual NOT_FOUND
    }
  }

  "must redirect to Unauthorised page for a DELETE when user is not a team member or administrator" in {
    val fixture = buildFixture(userModel = FakeUserNotTeamMember)

    when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(false))(any()))
      .thenReturn(Future.successful(Some(FakeApplication)))

    running(fixture.playApplication) {
      val request = FakeRequest(POST, routes.ApplicationDetailsController.delete(FakeApplication.id).url)

      val result = route(fixture.playApplication, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
    }
  }

}

object ApplicationDetailsControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  def buildFixture(userModel: UserModel = FakeUser, testConfiguration: Configuration = Configuration.empty): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel, testConfiguration)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
