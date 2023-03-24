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
import controllers.ApplicationDetailsControllerSpec.buildFixture
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import views.html.ApplicationDetailsView

import scala.concurrent.Future

class ApplicationDetailsControllerSpec extends SpecBase with MockitoSugar {

  "ApplicationDetails Controller" - {

    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture()

      val id = "test-id"
      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.ApplicationDetailsController.onPageLoad(id).url)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(FakeApplication, Some(FakeUser))(request, messages(fixture.playApplication)).toString
      }
    }
  }

  "must return 404 Not Found when the application does not exist" in {
    val fixture = buildFixture()

    val id = "test-id"

    when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(id))(any()))
      .thenReturn(Future.successful(None))

    running(fixture.playApplication) {
      val request = FakeRequest(GET, routes.ApplicationDetailsController.onPageLoad(id).url)

      val result = route(fixture.playApplication, request).value
      status(result) mustBe NOT_FOUND
    }
  }
  "must redirect to Unauthorised page for a GET when user is not a team member" in {
    val fixture = buildFixture(user = FakeUserNotTeamMember)

    val id = "test-id"
    when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(id))(any()))
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
}




object ApplicationDetailsControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  def buildFixture(user: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(playApplication, apiHubService)
  }

}
