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
import config.{FrontendAppConfig, HipEnvironments}
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import fakes.FakeHipEnvironments
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.application.EnvironmentsView

import scala.concurrent.Future

class EnvironmentsControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  "Environments Controller" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(true), eqTo(false))(any()))
            .thenReturn(Future.successful(Some(FakeApplication)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "test").url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[EnvironmentsView]
            implicit val config: FrontendAppConfig = fixture.playApplication.injector.instanceOf[FrontendAppConfig]
            implicit val hipEnvironments: HipEnvironments = fixture.playApplication.injector.instanceOf[HipEnvironments]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(FakeApplication, user, FakeHipEnvironments.test)(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
            contentAsString(result) must include("""id="test-apis"""")
            contentAsString(result) must include("""id="test-credentials"""")
            contentAsString(result) must include("""id="test-curl"""")
          }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any(), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "test").url)
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

    "must return 404 Not Found when the environment does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(true), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "badEnvironment").url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Environment not found",
            s"Cannot find environment badEnvironment."
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
        val request = FakeRequest(GET, controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "test").url)
        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
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

