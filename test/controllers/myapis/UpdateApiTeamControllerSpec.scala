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

package controllers.myapis

import base.SpecBase
import controllers.actions.{ApiAuthActionProvider, FakeApiAuthActions, FakeApiDetail}
import controllers.routes
import forms.myapis.UpdateApiTeamFormProvider
import models.application.TeamMember
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.myapis.{UpdateApiTeamSuccessView, UpdateApiTeamView}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateApiTeamControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with HtmlValidation
    with TestHelpers
    with FakeApiAuthActions {

  val teamId = "team1"
  val allTeams = Seq(
    Team(teamId, "Team 1", LocalDateTime.now(), Seq(TeamMember("user@example.com")))
  )
  val form = new UpdateApiTeamFormProvider()()

  "onPageLoad" - {
    "must return 200 Ok and the correct view for a support user" in {
      forAll(usersWhoCanSupport) { user =>
        val fixture = buildFixture(user)
        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(allTeams))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onPageLoad(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[UpdateApiTeamView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(form, FakeApiDetail, None, allTeams, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
          contentAsString(result) must include("Select which team owns this API")
        }
      }
    }

    "must render the correct view with ordered teams" in {
      forAll(usersWhoCanSupport) { user =>
        val unorderedTeams = Seq(
          Team(teamId, "Team 1", LocalDateTime.now(), Seq(TeamMember("user@example.com"))),
          Team(teamId, "A Team 1", LocalDateTime.now(), Seq(TeamMember("user@example.com"))),
          Team(teamId, "team 1", LocalDateTime.now(), Seq(TeamMember("user@example.com"))),
          Team(teamId, "a team 1", LocalDateTime.now(), Seq(TeamMember("user@example.com")))
        )
        val orderedTeams = unorderedTeams.sortBy(_.name.toLowerCase())
        val fixture = buildFixture(user)
        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(unorderedTeams))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onPageLoad(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[UpdateApiTeamView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(form, FakeApiDetail, None, orderedTeams, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
          contentAsString(result) must include("Select which team owns this API")
        }
      }
    }

    "must return 200 Ok and the correct view for a non-support user who is on the API team" in {
      forAll(usersWhoCannotSupport) { user =>
        val fixture = buildFixture(user)
        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(allTeams))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onPageLoad(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[UpdateApiTeamView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(form, FakeApiDetail, None, allTeams, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
          contentAsString(result) must not include ("Select which team owns this API")
        }
      }
    }

    "must redirect to Unauthorised page for a non-support user not on the api team" in {
      forAll(usersWhoCannotSupport) { user =>
        val fixture = buildFixture(user)
        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(unauthorisedApiAuthAction())
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(allTeams))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onPageLoad(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  "onSubmit" - {
    "must return 200 Ok and the correct view for a support user when a team has been selected" in {
      forAll(usersWhoCanSupport) { user =>
        val fixture = buildFixture(user)
        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(allTeams))
        when(fixture.apiHubService.updateApiTeam(any, any)(any)).thenReturn(Future.successful(Some(())))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onSubmit(FakeApiDetail.id))
            .withFormUrlEncodedBody(("owningTeam", teamId))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[UpdateApiTeamSuccessView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(FakeApiDetail, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService).updateApiTeam(eqTo(FakeApiDetail.id), eqTo(teamId))(any)
        }
      }
    }

    "must return 400 when a team has not been selected" in {
      forAll(usersWhoCanSupport) { user =>
        val fixture = buildFixture(user)
        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(allTeams))
        when(fixture.apiHubService.updateApiTeam(any, any)(any)).thenReturn(Future.successful(Some(())))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onSubmit(FakeApiDetail.id))
            .withFormUrlEncodedBody(("owningTeam", ""))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[UpdateApiTeamView]

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(form.bind(Map("owningTeam" -> "")), FakeApiDetail, None, allTeams, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService, never).updateApiTeam(eqTo(FakeApiDetail.id), any)(any)
        }
      }
    }

    "must return a Not Found page when the api or team does not exist" in {
      forAll(usersWhoCanSupport) { user =>
        val fixture = buildFixture(user)

        when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
        when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(allTeams))
        when(fixture.apiHubService.updateApiTeam(any, any)(any)).thenReturn(Future.successful(None))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.UpdateApiTeamController.onSubmit(FakeApiDetail.id))
            .withFormUrlEncodedBody(("owningTeam", teamId))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

          status(result) mustBe NOT_FOUND
          contentAsString(result) mustBe
            view(
              "Page not found - 404",
              "Unable to update owning team for this API.",
              "The API or team cannot be found."
            )(request, messages(fixture.playApplication))
              .toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }
  }

  private case class Fixture(
                              playApplication: PlayApplication,
                              apiHubService: ApiHubService,
                              apiAuthActionProvider: ApiAuthActionProvider,
                            )

  private def buildFixture(user: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]
    val apiAuthActionProvider = mock[ApiAuthActionProvider]

    val playApplication = applicationBuilder(user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ApiAuthActionProvider].toInstance(apiAuthActionProvider)
      )
      .build()
    Fixture(playApplication, apiHubService, apiAuthActionProvider)
  }

}
