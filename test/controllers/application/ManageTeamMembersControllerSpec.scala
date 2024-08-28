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
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import models.application.TeamMember
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
import views.html.ErrorTemplate
import views.html.application.ManageTeamMembersView

import scala.concurrent.Future

class ManageTeamMembersControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  "ManageTeamMembersController.onPageLoad" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any))
            .thenReturn(Future.successful(Some(FakeApplication)))

          running(fixture.playApplication) {
            val request = FakeRequest(controllers.application.routes.ManageTeamMembersController.onPageLoad(FakeApplication.id))
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[ManageTeamMembersView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(FakeApplication, user)(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must sort the application's team members alphabetically" in {
      val application = FakeApplication.copy(
        teamMembers = Seq(
          TeamMember(email = FakeUser.email.value),
          TeamMember(email = "cc@hmrc.gov.uk"),
          TeamMember(email = "ab@hmrc.gov.uk"),
          TeamMember(email = "aa@hmrc.gov.uk"),
          TeamMember(email = "Zb@hmrc.gov.uk"),
          TeamMember(email = "za@hmrc.gov.uk")
        )
      )

      val expected = FakeApplication.copy(
        teamMembers = Seq(
          TeamMember(email = "aa@hmrc.gov.uk"),
          TeamMember(email = "ab@hmrc.gov.uk"),
          TeamMember(email = "cc@hmrc.gov.uk"),
          TeamMember(email = FakeUser.email.value),
          TeamMember(email = "za@hmrc.gov.uk"),
          TeamMember(email = "Zb@hmrc.gov.uk")
        )
      )

      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(application.id), any, any)(any))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.ManageTeamMembersController.onPageLoad(application.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ManageTeamMembersView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(expected, FakeUser)(request, messages(fixture.playApplication)).toString
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any, any)(any))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.ManageTeamMembersController.onPageLoad(FakeApplication.id))
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

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.ManageTeamMembersController.onPageLoad(FakeApplication.id))
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
