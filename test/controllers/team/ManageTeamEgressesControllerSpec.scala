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
import config.CryptoProvider
import controllers.actions.{FakeApplication, FakeSupporter, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import fakes.FakeHipEnvironments
import models.api.EgressGateway
import models.application.TeamMember
import models.team.Team
import models.team.TeamLenses.*
import models.user.{LdapUser, UserModel}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.team.ManageTeamEgressesView

import java.time.LocalDateTime
import scala.concurrent.Future

class ManageTeamEgressesControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  "ManageTeamEgressesController.onPageLoad" - {
    "must return OK and the correct view for a support user" in {
      val fixture = buildFixture(FakeSupporter)

      val egressGateways = Seq(
        EgressGateway("test-egress-1", "Test Egress 1"),
        EgressGateway("test-egress-2", "Test Egress 2")
      )
      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(TeamMember(FakeSupporter.email)),
        egresses = egressGateways.take(1).map(_.id)
      )

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))
      when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.ManageTeamEgressesController.onPageLoad(team.id))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK

        val view = fixture.playApplication.injector.instanceOf[ManageTeamEgressesView]
        contentAsString(result) mustBe view(team, FakeSupporter, egressGateways.take(1))(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeamById(eqTo(team.id))(any)
        verify(fixture.apiHubService).listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)
      }
    }

    "must return OK and the correct view for a non support user" in {
      val fixture = buildFixture(FakeUser)

      val egressGateways = Seq(
        EgressGateway("test-egress-1", "Test Egress 1"),
        EgressGateway("test-egress-2", "Test Egress 2")
      )
      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(TeamMember(FakeUser.email)),
        egresses = egressGateways.take(1).map(_.id)
      )

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))
      when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.ManageTeamEgressesController.onPageLoad(team.id))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK

        val view = fixture.playApplication.injector.instanceOf[ManageTeamEgressesView]
        contentAsString(result) mustBe view(team, FakeUser, egressGateways.take(1))(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeamById(eqTo(team.id))(any)
        verify(fixture.apiHubService).listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)
      }
    }

    "must return 404 Not Found when the team does not exist" in {
      val teamId = "test-team-id"
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.ManageTeamEgressesController.onPageLoad(teamId))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe NOT_FOUND

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with ID $teamId.",
            Some(FakeSupporter)
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
