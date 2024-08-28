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
import controllers.actions.{FakeSupporter, FakeUser, FakeUserNotTeamMember}
import controllers.team
import forms.ChangeTeamNameFormProvider
import models.application.TeamMember
import models.exception.TeamNameNotUniqueException
import models.team.Team
import models.user.UserModel
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation
import views.html.team.{ChangeTeamNameView, TeamUpdatedSuccessfullyView}

import java.time.LocalDateTime
import scala.concurrent.Future

class ChangeTeamNameControllerSpec extends SpecBase with MockitoSugar with HtmlValidation{

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ChangeTeamNameFormProvider()
  private val form = formProvider()
  private val id = "a_team_id"
  private val teamMember = TeamMember.apply(FakeUser.email.get)
  val testTeam = Team.apply(id, "team name", LocalDateTime.now(), Seq(teamMember))

  private lazy val changeTeamNameRoutePageLoad = team.routes.ChangeTeamNameController.onPageLoad(id).url
  private lazy val changeTeamNameRouteOnSubmit = team.routes.ChangeTeamNameController.onSubmit(id).url

  "onPageLoad" - {

    "must return OK and the correct view for a GET when the user is a team member" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(GET, changeTeamNameRoutePageLoad)

        when(fixture.apiHubService.findTeamById(eqTo(id))(any)).thenReturn(Future.successful(Some(testTeam)))

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ChangeTeamNameView]

        redirectLocation(result) mustBe None
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("team name"), controllers.team.routes.ChangeTeamNameController.onSubmit(id), FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET when the user is a supporter but not on the team" in {
      val fixture = buildFixture(userModel = FakeSupporter)

      running(fixture.application) {
        val request = FakeRequest(GET, changeTeamNameRoutePageLoad)

        when(fixture.apiHubService.findTeamById(eqTo(id))(any)).thenReturn(Future.successful(Some(testTeam)))

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ChangeTeamNameView]

        redirectLocation(result) mustBe None
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("team name"), controllers.team.routes.ChangeTeamNameController.onSubmit(id), FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return NOT_FOUND and the team not found view for a GET" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(GET, changeTeamNameRoutePageLoad)

        when(fixture.apiHubService.findTeamById(eqTo(id))(any)).thenReturn(Future.successful(None))

        val result = route(fixture.application, request).value

        redirectLocation(result) mustBe None
        status(result) mustEqual NOT_FOUND
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(testTeam)))

      running(fixture.application) {
        val request = FakeRequest(GET, changeTeamNameRoutePageLoad)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  "onSubmit" - {

    "must return a Bad Request and errors on a POST when the new name is not unique" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.findTeamByName(any)(any)).thenReturn(Future.successful(Some(testTeam)))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(testTeam)))
      when(fixture.apiHubService.changeTeamName(any, any)(any)).thenReturn(Future.successful(Left(TeamNameNotUniqueException.forName(testTeam.name))))

      running(fixture.application) {
        val error = FormError("value", "createTeamName.error.nameNotUnique")
        val request = FakeRequest(POST, changeTeamNameRouteOnSubmit).withFormUrlEncodedBody(("value", "team name"),(error.key, error.message))

        val view = fixture.application.injector.instanceOf[ChangeTeamNameView]

        val formWithErrors = form.fill(testTeam.name).withError(error)

        val result = route(fixture.application, request).value

        contentAsString(result) mustEqual view(formWithErrors, controllers.team.routes.ChangeTeamNameController.onSubmit(id), FakeUser)(request, messages(fixture.application)).toString
        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).changeTeamName(eqTo(id), eqTo(testTeam.name))(any)
      }
    }

    "must show the success view when valid data is submitted" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.findTeamByName(any)(any)).thenReturn(Future.successful(Some(testTeam)))
      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(testTeam)))
      when(fixture.apiHubService.changeTeamName(any, any)(any)).thenReturn(Future.successful(Right(())))

      running(fixture.application) {
        val request =
          FakeRequest(POST, changeTeamNameRouteOnSubmit)
            .withFormUrlEncodedBody(("value", "answer"))

        val view = fixture.application.injector.instanceOf[TeamUpdatedSuccessfullyView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }


  private case class Fixture(
    application: Application,
    apiHubService: ApiHubService
  )

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = userModel)
      .overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}
