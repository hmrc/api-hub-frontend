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

package controllers.application

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.{FakeApplication, FakePrivilegedUser, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import models.application.{Primary, Secondary}
import models.application.ApplicationLenses.*
import models.exception.ApplicationCredentialLimitException
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.application.EnvironmentAndCredentialsView
import fakes.FakeHipEnvironments
import scala.concurrent.Future

class EnvironmentAndCredentialsControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  "EnvironmentAndCredentials Controller" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(true), eqTo(false))(any()))
            .thenReturn(Future.successful(Some(FakeApplication)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(FakeApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[EnvironmentAndCredentialsView]
            implicit val config: FrontendAppConfig = fixture.playApplication.injector.instanceOf[FrontendAppConfig]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(FakeApplication, user, config.helpDocsPath)(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any(), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with ID ${FakeApplication.id}."
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(true), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  "deletePrimaryCredential" - {
    "must delete the credential and redirect to the production tab" in {
      val clientId = "test-client-id"

      forAll(usersWhoCanDeletePrimaryCredentials) {(user: UserModel) =>
        val application = FakeApplication.addTeamMember(user.email)
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
          .thenReturn(Future.successful(Some(application)))

        when(fixture.apiHubService.deleteCredential(any(), any(), any())(any())).thenReturn(Future.successful(Right(Some(()))))

        running(fixture.playApplication) {
          val url = controllers.application.routes.EnvironmentAndCredentialsController.deletePrimaryCredential(application.id, clientId) .url
          val request = FakeRequest(GET, url)
          val result = route(fixture.playApplication, request).value

          val expectedUrl = controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(application.id).url + "#hip-production"

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(expectedUrl)
          verify(fixture.apiHubService).deleteCredential(
            eqTo(application.id),
            eqTo(FakeHipEnvironments.production),
            eqTo(clientId))(any()
          )
        }
      }
    }

    "must redirect to the unauthorised page for users who cannot delete production credentials" in {
      forAll(usersWhoCannotDeletePrimaryCredentials){(user: UserModel) =>
        val application = FakeApplication.addTeamMember(user.email)
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
          .thenReturn(Future.successful(Some(application)))

        running(fixture.playApplication) {
          val url = controllers.application.routes.EnvironmentAndCredentialsController.deletePrimaryCredential(application.id, "test-client-id").url
          val request = FakeRequest(GET, url)
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must display a Not Found page when the credential does not exist" in {
      val clientId = "test-client-id"

      val application = FakeApplication.addTeamMember(FakePrivilegedUser.email)
      val fixture = buildFixture(FakePrivilegedUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.deleteCredential(any(), any(), any())(any())).thenReturn(Future.successful(Right(None)))

      running(fixture.playApplication) {
        val url = controllers.application.routes.EnvironmentAndCredentialsController.deletePrimaryCredential(application.id, clientId) .url
        val request = FakeRequest(GET, url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Credential not found",
            s"Cannot find credential with ID $clientId for application ${application.id}."
          )(request, messages(fixture.playApplication))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must display a Bad Request page when the user attempts to delete the last credential" in {
      val clientId = "test-client-id"

      val application = FakeApplication.addTeamMember(FakePrivilegedUser.email)
      val fixture = buildFixture(FakePrivilegedUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.deleteCredential(any(), any(), any())(any()))
        .thenReturn(Future.successful(Left(ApplicationCredentialLimitException.forId(FakeApplication.id, Primary))))

      running(fixture.playApplication) {
        val url = controllers.application.routes.EnvironmentAndCredentialsController.deletePrimaryCredential(application.id, clientId) .url
        val request = FakeRequest(GET, url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Cannot revoke last credential",
            "You cannot revoke the last credential for an application."
          )(request, messages(fixture.playApplication))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }
  }

  "deleteSecondaryCredential" - {
    "must delete the credential and redirect to the development tab" in {
      val clientId = "test-client-id"

      forAll(usersWhoCanDeleteSecondaryCredentials) {(user: UserModel) =>
        val application = FakeApplication.addTeamMember(user.email)
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
          .thenReturn(Future.successful(Some(application)))

        when(fixture.apiHubService.deleteCredential(any(), any(), any())(any())).thenReturn(Future.successful(Right(Some(()))))

        running(fixture.playApplication) {
          val url = controllers.application.routes.EnvironmentAndCredentialsController.deleteSecondaryCredential(application.id, clientId) .url
          val request = FakeRequest(GET, url)
          val result = route(fixture.playApplication, request).value

          val expectedUrl = controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(application.id).url + "#hip-development"

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(expectedUrl)
          verify(fixture.apiHubService).deleteCredential(
            eqTo(application.id),
            eqTo(FakeHipEnvironments.test),
            eqTo(clientId))(any()
          )
        }
      }
    }

    "must redirect to the unauthorised page for users who cannot delete development credentials" in {
      forAll(usersWhoCannotDeleteSecondaryCredentials){(user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any(), any())(any()))
          .thenReturn(Future.successful(Some(FakeApplication)))

        running(fixture.playApplication) {
          val url = controllers.application.routes.EnvironmentAndCredentialsController.deleteSecondaryCredential(FakeApplication.id, "test-client-id").url
          val request = FakeRequest(GET, url)
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must display a Not Found page when the credential does not exist" in {
      val clientId = "test-client-id"

      val application = FakeApplication.addTeamMember(FakePrivilegedUser.email)
      val fixture = buildFixture(FakePrivilegedUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.deleteCredential(any(), any(), any())(any())).thenReturn(Future.successful(Right(None)))

      running(fixture.playApplication) {
        val url = controllers.application.routes.EnvironmentAndCredentialsController.deleteSecondaryCredential(application.id, clientId) .url
        val request = FakeRequest(GET, url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Credential not found",
            s"Cannot find credential with ID $clientId for application ${application.id}."
          )(request, messages(fixture.playApplication))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must display a Bad Request page when the user attempts to delete the last credential" in {
      val clientId = "test-client-id"

      val application = FakeApplication.addTeamMember(FakePrivilegedUser.email)
      val fixture = buildFixture(FakePrivilegedUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.deleteCredential(any(), any(), any())(any()))
        .thenReturn(Future.successful(Left(ApplicationCredentialLimitException.forId(FakeApplication.id, Primary))))

      running(fixture.playApplication) {
        val url = controllers.application.routes.EnvironmentAndCredentialsController.deleteSecondaryCredential(application.id, clientId) .url
        val request = FakeRequest(GET, url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Cannot revoke last credential",
            "You cannot revoke the last credential for an application."
          )(request, messages(fixture.playApplication))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
