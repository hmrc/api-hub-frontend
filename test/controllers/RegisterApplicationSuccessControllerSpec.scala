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
import controllers.RegisterApplicationSuccessControllerSpec.buildFixture
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import models.application.{Application, Creator, TeamMember}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.RegisterApplicationSuccessView

import scala.concurrent.Future

class RegisterApplicationSuccessControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  "RegisterApplicationSuccess Controller" - {

    "must return OK and the correct view for a GET for a team member or supporter" in {
      val app = Application("id-1", "test", Creator("creator-email"), Seq(TeamMember("test-email")))

      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(user)

          when(fixture.apiHubService.getApplication(eqTo("id-1"), eqTo(false))(any()))
            .thenReturn(Future.successful(Some(app)))

          running(fixture.application) {
            val request = FakeRequest(GET, routes.RegisterApplicationSuccessController.onPageLoad("id-1").url)

            val result = route(fixture.application, request).value

            val view = fixture.application.injector.instanceOf[RegisterApplicationSuccessView]

            status(result) mustEqual OK

            val expected = view(app, Some(user))(request, messages(fixture.application)).toString
            val actual = contentAsString(result)
            actual mustEqual expected
            actual must validateAsHtml
          }
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val testId = "test-app-id"
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(eqTo(testId), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.RegisterApplicationSuccessController.onPageLoad(testId).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER

        val actualRedirectLocation = redirectLocation(result).value
        val expectedRedirectLocation = routes.UnauthorisedController.onPageLoad.url

        actualRedirectLocation mustEqual expectedRedirectLocation
      }
    }

  }
}
object RegisterApplicationSuccessControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
                      application: PlayApplication,
                      apiHubService: ApiHubService
                    )

  def buildFixture( userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = None, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}