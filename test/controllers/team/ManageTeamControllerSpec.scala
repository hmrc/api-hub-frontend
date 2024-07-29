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

package controllers.team

import base.SpecBase
import controllers.actions.{FakeUser, FakeUserNotTeamMember}
import controllers.routes
import models.application.TeamMember
import models.team.Team
import models.team.TeamLenses._
import models.user.{LdapUser, UserModel}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.team.ManageTeamView

import java.time.LocalDateTime
import scala.concurrent.Future

class ManageTeamControllerSpec extends SpecBase with MockitoSugar with ArgumentMatchersSugar with TestHelpers with HtmlValidation {

  "ManageTeamController.onPageLoad" - {
    "must return OK and the correct view for a team member or supporter" in {
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))

      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

          running(fixture.playApplication) {
            val request = FakeRequest(controllers.team.routes.ManageTeamController.onPageLoad(team.id))
            val result = route(fixture.playApplication, request).value

            status(result) mustBe OK

            val view = fixture.playApplication.injector.instanceOf[ManageTeamView]
            contentAsString(result) mustBe view(team, None, user)(request, messages(fixture.playApplication)).toString()
            contentAsString(result) must validateAsHtml

            verify(fixture.apiHubService).findTeamById(eqTo(team.id))(any)
          }
      }
    }

    "must sort the team members alphabetically" in {
      val email1 = "test-email-1"
      val email2 = "test-email-2"
      val email3 = "test-email-3"

      val user = UserModel("test-user-id", "test-user-name", LdapUser, Some(email1))

      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(
          TeamMember(email3),
          TeamMember(email1),
          TeamMember(email2)
        )
      )

      val fixture = buildFixture(userModel = user)

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.ManageTeamController.onPageLoad(team.id))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK

        val view = fixture.playApplication.injector.instanceOf[ManageTeamView]

        val sortedTeam = team.setTeamMembers(
          Seq(
            TeamMember(email1),
            TeamMember(email2),
            TeamMember(email3)
          )
        )

        contentAsString(result) mustBe view(sortedTeam, None, user)(request, messages(fixture.playApplication)).toString()
      }
    }

    "must return 404 Not Found when the team does not exist" in {
      val teamId = "test-team-id"
      val fixture = buildFixture()

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.ManageTeamController.onPageLoad(teamId))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe NOT_FOUND

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with Id $teamId."
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page when the user is not a team member or supporter" in {
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))
      val fixture = buildFixture(FakeUserNotTeamMember)

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.ManageTeamController.onPageLoad(team.id))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
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
