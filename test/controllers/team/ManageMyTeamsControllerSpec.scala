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
import controllers.actions.FakeUser
import models.application.TeamMember
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OptionValues
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.HtmlValidation
import views.html.team.ManageMyTeamsView

import java.time.LocalDateTime
import scala.concurrent.Future

class ManageMyTeamsControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with OptionValues {

  "ManageMyTeamsController" - {
    "must return OK and the correct view for a GET when the user is in some teams" in {
      val fixture = buildFixture()
      val team1 = Team("id1", "team 1", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))
      val team2 = Team("id2", "Team 2", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))
      val team3 = Team("id3", "team 2", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))
      val team4 = Team("id4", "A team 2", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))
      val team5 = Team("id5", "a team 2", LocalDateTime.now(), Seq(TeamMember(FakeUser.email.value)))
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(Seq(team2, team1, team3, team4, team5)))

      running(fixture.playApplication) {
        val request = FakeRequest(routes.ManageMyTeamsController.onPageLoad())
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ManageMyTeamsView]
        val teamsSortedByName = Seq(team4, team5, team1, team2, team3)

        status(result) mustBe OK
        contentAsString(result) mustBe view(teamsSortedByName, FakeUser)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must show No Teams message when the user is not in any teams" in {
      val fixture = buildFixture()
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(Seq()))

      running(fixture.playApplication) {
        val request = FakeRequest(routes.ManageMyTeamsController.onPageLoad())
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ManageMyTeamsView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(Seq.empty, FakeUser)(request, messages(fixture.playApplication)).toString
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
