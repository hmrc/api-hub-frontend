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
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.application.ApplicationApisView

import scala.concurrent.Future

class ApplicationApisControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  "ApplicationApisController" - {
    "must return OK and the correct view for a GET for a team member or administrator" in {
      forAll(teamMemberAndAdministratorTable) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(true))(any()))
            .thenReturn(Future.successful(Some(FakeApplication)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.ApplicationApisController.onPageLoad(FakeApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[ApplicationApisView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(FakeApplication, Some(user))(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationApisController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with Id ${FakeApplication.id}."
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or administrator" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationApisController.onPageLoad(FakeApplication.id).url)
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