/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.actions.{FakeApplication, FakeSupporter}
import fakes.FakeHipEnvironments
import models.application.{CredentialScopes, Primary}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.application.AllScopesView

import java.time.LocalDateTime
import scala.concurrent.Future

class AllScopesControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation {

  import AllScopesControllerSpec.*

  "onPageLoad" - {
    "must return the correct view for an application that exists for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any))
          .thenReturn(Future.successful(Some(FakeApplication)))

        when(fixture.apiHubService.fetchAllScopes(eqTo(FakeApplication.id))(any))
          .thenReturn(Future.successful(Some(credentialScopes)))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.AllScopesController.onPageLoad(FakeApplication.id))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[AllScopesView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(FakeApplication, credentialScopes, user)(request, messages(fixture.playApplication)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must redirect to the unauthorised page for a user who is not support" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.AllScopesController.onPageLoad(FakeApplication.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.AllScopesController.onPageLoad(FakeApplication.id))
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
  }

  "fixScopes" - {
    "must fix an application's scopes and redirect back to the All scopes page for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any))
          .thenReturn(Future.successful(Some(FakeApplication)))

        when(fixture.apiHubService.fixScopes(eqTo(FakeApplication.id))(any))
          .thenReturn(Future.successful(Some(())))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.AllScopesController.fixScopes(FakeApplication.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.application.routes.AllScopesController.onPageLoad(FakeApplication.id).url)
        }
      }
    }

    "must redirect to the unauthorised page for a user who is not support" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.AllScopesController.fixScopes(FakeApplication.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.AllScopesController.fixScopes(FakeApplication.id))
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
  }

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}

object AllScopesControllerSpec {

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private val credentialScopes = (1 to 2).map(
    i =>
      CredentialScopes(
        environmentId = FakeHipEnvironments.production.id,
        clientId = s"test-client-id-$i",
        created = LocalDateTime.now(),
        scopes = Seq(s"test-scope-$i-1", s"test-scope-$i-2")
      )
  )

}
