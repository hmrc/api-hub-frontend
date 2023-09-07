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
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import forms.ProductionCredentialsChecklistFormProvider
import models.application.{Application, Credential, Secret}
import models.application.ApplicationLenses.ApplicationLensOps
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.{Configuration, Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.TestHelpers
import viewmodels.GeneratePrimarySecretSuccessViewModel
import views.html.{ErrorTemplate, GeneratePrimarySecretSuccessView, ProductionCredentialsChecklistView}

import scala.concurrent.Future

class ProductionCredentialsChecklistControllerSpec extends SpecBase with MockitoSugar with TestHelpers {

  import ProductionCredentialsChecklistControllerSpec._

  private val formProvider = new ProductionCredentialsChecklistFormProvider()
  private val form = formProvider()

  private lazy val productionCredentialsChecklistRoute = routes.ProductionCredentialsChecklistController.onPageLoad(FakeApplication.id).url

  "ProductionCredentialsChecklist Controller" - {

    "must return OK and the correct view for a GET for a team member or administrator" in {
      forAll(teamMemberAndAdministratorTable) {
        user =>
          val fixture = buildFixture(user)

          running(fixture.playApplication) {
            val request = FakeRequest(GET, productionCredentialsChecklistRoute)

            val result = route(fixture.playApplication, request).value

            val view = fixture.playApplication.injector.instanceOf[ProductionCredentialsChecklistView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, FakeApplication.id)(request, messages(fixture.playApplication)).toString
          }
      }
    }

    "must return the Generate Primary Secret Success view when valid data is submitted by a team member or administrator" in {
      forAll(teamMemberAndAdministratorTable) {
        user =>
          val fixture = buildFixture(user)
          val secret = Secret("test-secret")

          when(fixture.apiHubService.createPrimarySecret(ArgumentMatchers.eq(applicationWithValidPrimaryCredential.id))(any()))
            .thenReturn(Future.successful(Some(secret)))

          running(fixture.playApplication) {
            val request =
              FakeRequest(POST, productionCredentialsChecklistRoute)
                .withFormUrlEncodedBody(("value[0]", "confirm"))

            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[GeneratePrimarySecretSuccessView]
            val frontendAppConfig = fixture.playApplication.injector.instanceOf[FrontendAppConfig]

            val summaryList = GeneratePrimarySecretSuccessViewModel.buildSummary(
              applicationWithValidPrimaryCredential,
              frontendAppConfig.environmentNames,
              applicationWithValidPrimaryCredential.getPrimaryCredentials.head,
              secret
            )(messages(fixture.playApplication))

            status(result) mustEqual OK
            contentAsString(result) mustBe view(applicationWithValidPrimaryCredential, summaryList, Some(user), secret.secret)(request, messages(fixture.playApplication)).toString()
          }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture()

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, productionCredentialsChecklistRoute)
            .withFormUrlEncodedBody(("value[0]", ""))

        val boundForm = form.bind(Map("value[0]" -> ""))

        val view = fixture.playApplication.injector.instanceOf[ProductionCredentialsChecklistView]

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, FakeApplication.id)(request, messages(fixture.playApplication)).toString
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or administrator" in {
      val fixture = buildFixture(FakeUserNotTeamMember)

      running(fixture.playApplication) {
        val request = FakeRequest(GET, productionCredentialsChecklistRoute)

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must redirect to Unauthorised page for a POST when user is not a team member or administrator" in {
      val fixture = buildFixture(FakeUserNotTeamMember)

      running(fixture.playApplication) {
        val request = FakeRequest(POST, productionCredentialsChecklistRoute)
          .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must return Bad Request when the primary credentials are invalid for a GET" in {
      val fixture = buildFixture(application = Some(applicationWithoutValidCredential))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, productionCredentialsChecklistRoute)

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Invalid primary credential",
            "This application does not have a valid primary credential to generate a secret for."
          )(request, messages(fixture.playApplication))
            .toString()
      }
    }

    "must return Bad Request when the primary credentials are invalid for a POST" in {
      val fixture = buildFixture(application = Some(applicationWithoutValidCredential))

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, productionCredentialsChecklistRoute)
            .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Invalid primary credential",
            "This application does not have a valid primary credential to generate a secret for."
          )(request, messages(fixture.playApplication))
            .toString()
      }
    }

    "must return Bad Request when the primary credential secret has already been generated for a GET" in {
      val fixture = buildFixture(application = Some(applicationWithPrimaryCredentialWithSecret))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, productionCredentialsChecklistRoute)

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Secret already generated",
            "A primary credential secret has already been generated for this application."
          )(request, messages(fixture.playApplication))
            .toString()
      }
    }

    "must return Bad Request when the primary credential secret has already been generated for a POST" in {
      val fixture = buildFixture(application = Some(applicationWithPrimaryCredentialWithSecret))

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, productionCredentialsChecklistRoute)
            .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Secret already generated",
            "A primary credential secret has already been generated for this application."
          )(request, messages(fixture.playApplication))
            .toString()
      }
    }

    "must return Bad Request when IDMS cannot find the primary credential for a POST" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.createPrimarySecret(ArgumentMatchers.eq(applicationWithValidPrimaryCredential.id))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, productionCredentialsChecklistRoute)
            .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Primary credential not found",
            "This application's primary credential cannot be found in IDMS."
          )(request, messages(fixture.playApplication))
            .toString()
      }
    }
  }

}

object ProductionCredentialsChecklistControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  def buildFixture(userModel: UserModel = FakeUser, application: Option[Application] = Some(applicationWithValidPrimaryCredential)): Fixture = {
    val apiHubService = mock[ApiHubService]
    when(apiHubService.getApplication(any(), any())(any()))
      .thenReturn(Future.successful(application))

    val configuration = Configuration.from(Map(
      "environment-names.primary" -> "test-primary",
      "environment-names.secondary" -> "test-secondary"
    ))

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel, configuration)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

  val applicationWithValidPrimaryCredential: Application = FakeApplication
    .setPrimaryCredentials(Seq(Credential("test-client-id", None, None)))

  val applicationWithoutValidCredential: Application = FakeApplication

  val applicationWithPrimaryCredentialWithSecret: Application = FakeApplication
    .setPrimaryCredentials(Seq(Credential("test-client-id", None, Some("test-secret-fragment"))))

}
