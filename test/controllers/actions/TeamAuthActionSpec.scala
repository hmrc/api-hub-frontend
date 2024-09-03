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

package controllers.actions

import base.SpecBase
import models.application.TeamMember
import models.requests.{IdentifierRequest, TeamRequest}
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.ErrorTemplate

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamAuthActionSpec extends SpecBase with Matchers with MockitoSugar {

  "TeamAuthAction" - {
    "must grant a user access to a team when they are a team member" in {
      val fixture = buildFixture()
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email)))

      when(fixture.apiHubService.findTeamById(eqTo(team.id))(any)).thenReturn(Future.successful(Some(team)))

      val result = fixture.provider.apply(team.id).invokeBlock(buildRequest(), buildInvokeBlock())
      status(result) mustBe OK
    }

    "must grant a user access to a team when they are a supporter but not a team member" in {
      val fixture = buildFixture()
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq.empty)

      when(fixture.apiHubService.findTeamById(eqTo(team.id))(any)).thenReturn(Future.successful(Some(team)))

      val result = fixture.provider.apply(team.id).invokeBlock(buildRequest(FakeSupporter), buildInvokeBlock())
      status(result) mustBe OK
    }

    "must redirect to the Unauthorised page when the user is not a team member or supporter" in {
      val fixture = buildFixture()
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq.empty)

      when(fixture.apiHubService.findTeamById(eqTo(team.id))(any)).thenReturn(Future.successful(Some(team)))

      val result = fixture.provider.apply(team.id).invokeBlock(buildRequest(), buildInvokeBlock())
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }

    "must return Not Found with a suitable message when the team does not exist" in {
      val fixture = buildFixture()
      val teamId = "test-team-id"

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val result = fixture.provider.apply(teamId).invokeBlock(buildRequest(), buildInvokeBlock())

        status(result) mustBe NOT_FOUND

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with Id $teamId."
          )(FakeRequest(), messages(fixture.playApplication))
            .toString()
      }
    }
  }

  private case class Fixture(
    provider: TeamAuthActionProvider,
    apiHubService: ApiHubService,
    playApplication: Application
  )

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
      )
      .build()

    val provider = playApplication.injector.instanceOf[TeamAuthActionProvider]
    Fixture(provider, apiHubService, playApplication)
  }

  private def buildInvokeBlock[A](): TeamRequest[A] => Future[Result] = {
    _ => Future.successful(Results.Ok)
  }

  private def buildRequest(user: UserModel = FakeUser): IdentifierRequest[AnyContentAsEmpty.type] = {
    IdentifierRequest(FakeRequest(), user)
  }

}
