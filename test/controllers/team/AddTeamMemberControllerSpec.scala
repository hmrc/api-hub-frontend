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
import forms.AddTeamMemberDetailsFormProvider
import models.application.TeamMember
import models.team.Team
import models.team.TeamLenses._
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OptionValues
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.AddTeamMemberDetailsView
import views.html.team.TeamUpdatedSuccessfullyView

import java.time.LocalDateTime
import scala.concurrent.Future

class AddTeamMemberControllerSpec
  extends SpecBase
    with MockitoSugar
    with HtmlValidation
    with OptionValues
    with TestHelpers {

  import AddTeamMemberControllerSpec._

  "onPageLoad" - {
    "must return OK and the correct view for a GET" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

          running(fixture.playApplication) {
            val request = FakeRequest(controllers.team.routes.AddTeamMemberController.onPageLoad(teamId))
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[AddTeamMemberDetailsView]

            status(result) mustBe OK
            contentAsString(result) mustBe view(form, submitTo(teamId), user)(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.AddTeamMemberController.onPageLoad(teamId))
        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  "onSubmit" - {
    "must add the team member and redirect to the Manage Team Members page when valid data is submitted" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(userModel = user)

          val email = "test.email@hmrc.gov.uk"

          when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

          when(fixture.apiHubService.addTeamMemberToTeam(any, any)(any))
            .thenReturn(Future.successful(Some(())))

          running(fixture.playApplication) {
            val request = FakeRequest(controllers.team.routes.AddTeamMemberController.onSubmit(teamId))
              .withFormUrlEncodedBody(("email", email))
            val result = route(fixture.playApplication, request).value

            status(result) mustBe OK

            val successView = fixture.playApplication.injector.instanceOf[TeamUpdatedSuccessfullyView]
            contentAsString(result) mustBe successView(user)(request, messages(fixture.playApplication)).toString()
            contentAsString(result) must validateAsHtml

            verify(fixture.apiHubService).addTeamMemberToTeam(eqTo(teamId), eqTo(TeamMember(email)))(any)
          }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.AddTeamMemberController.onSubmit(teamId))
          .withFormUrlEncodedBody(("email", ""))
        val result = route(fixture.playApplication, request).value
        val formWithErrors = form.bind(Map("email" -> ""))
        val view = fixture.playApplication.injector.instanceOf[AddTeamMemberDetailsView]

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(formWithErrors, submitTo(teamId), FakeUser)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return a Bad Request and errors when a duplicate team member is submitted" in {
      val fixture = buildFixture()

      val email = "test.email@hmrc.gov.uk"

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team.addTeamMember(email))))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.AddTeamMemberController.onSubmit(teamId))
          .withFormUrlEncodedBody(("email", email))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[AddTeamMemberDetailsView]

        val formWithErrors = form
          .fill(TeamMember(email))
          .withError(FormError("email", "addTeamMemberDetails.email.duplicate"))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(formWithErrors, submitTo(teamId), FakeUser)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }

    }

    "must redirect to Unauthorised page for a POST when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      val email = "test.email@hmrc.gov.uk"

      when(fixture.apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.team.routes.AddTeamMemberController.onSubmit(teamId))
          .withFormUrlEncodedBody(("email", email))
        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
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

  private def submitTo(id: String): Call = {
    controllers.team.routes.AddTeamMemberController.onSubmit(id)
  }

}

object AddTeamMemberControllerSpec extends OptionValues {

  private val formProvider = new AddTeamMemberDetailsFormProvider()
  private val form = formProvider()

  private val teamId = "test-team-id"
  private val team = Team(teamId, "test-team.name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email)))

}
