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

package controllers.admin

import base.SpecBase
import controllers.routes
import models.application.{Application, Creator, TeamMember}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.ManageApplicationsView

import scala.concurrent.Future

class ManageApplicationsControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation {

  "ManageApplicationsController" - {
    "must return the sorted list of applications to a user with the support role" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val testEmail = "test-email"
        val creatorEmail = "creator-email-2"
        val applications = Seq(
          Application("id-1", "app-name-2", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail))),
          Application("id-2", "app-name-1", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail)))
        )

        when(fixture.apiHubService.getApplications(any, any)(any)).thenReturn(Future.successful(applications))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.admin.routes.ManageApplicationsController.onPageLoad())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe OK

          verify(fixture.apiHubService).getApplications(eqTo(None), eqTo(true))(any)

          val view = fixture.playApplication.injector.instanceOf[ManageApplicationsView]
          contentAsString(result) mustBe view(applications.sortBy(_.name.toLowerCase), user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
          contentAsString(result) must include(s"""Manage applications (<span id="appCount">${applications.size}</span>)""")
        }
      }
    }

    "must redirect to the unauthorised page for a user who is not support" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.admin.routes.ManageApplicationsController.onPageLoad())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
