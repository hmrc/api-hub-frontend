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

package controllers.team

import base.SpecBase
import config.CryptoProvider
import forms.YesNoFormProvider
import models.UserAnswers
import models.application.TeamMember
import models.team.Team
import models.user.{LdapUser, UserModel}
import org.mockito.ArgumentMatchers.{any, matches, same}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.CreateTeamMembersPage
import play.api.{Application => PlayApplication}
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.inject
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.team.{RemoveTeamMemberConfirmationView, RemoveTeamMemberSuccessView}

import java.time.LocalDateTime
import scala.concurrent.Future
import uk.gov.hmrc.crypto.PlainText

class RemoveTeamMemberControllerSpec extends SpecBase with MockitoSugar with OptionValues with TryValues with HtmlValidation {

  "RemoveTeamMemberDetails Controller" - {
    "must navigate when successful removal of team member" in {
      val teamMember1 = TeamMember("creator@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")
      val userAnswers = UserAnswers(userAnswersId)
        .set(CreateTeamMembersPage, Seq(teamMember1, teamMember2))
        .success.value

      val mockSessionRepository = mock[CreateTeamSessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          inject.bind[CreateTeamSessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveTeamMemberController.removeTeamMember(1).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ManageTeamMembersController.onPageLoad().url

        val updatedAnswers = userAnswers
          .set(CreateTeamMembersPage, Seq(teamMember1))
          .success
          .value

        verify(mockSessionRepository).set(updatedAnswers)
      }
    }

    "must return not found when removal of creator team member" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(CreateTeamMembersPage, Seq(TeamMember("creator.email@hmrc.gov.uk"), TeamMember("team.member.email@hmrc.gov.uk")))
        .success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveTeamMemberController.removeTeamMember(0).url)

        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND
      }
    }

    "must return not found when removal of non-existent team member" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(CreateTeamMembersPage, Seq(TeamMember("creator.email@hmrc.gov.uk"), TeamMember("team.member.email@hmrc.gov.uk")))
        .success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveTeamMemberController.removeTeamMember(99).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "This page can’t be found",
            "Cannot find this team member."
          )(request, messages(application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return not found when no user answers" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveTeamMemberController.removeTeamMember(1).url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[ErrorTemplate]

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "This page can’t be found",
            "Cannot find this team member."
          )(request, messages(application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must render the success view on a successful removal of team member from an existing team" in {
      val teamMember1 = TeamMember("creator@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")

      val user = UserModel("test-user-id", "test-user-name", LdapUser, Some(teamMember1.email))

      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(
          teamMember1,
          teamMember2
        )
      )
      
      val fixture = buildFixture(user)
      val mockApiHubService = fixture.apiHubService
      
      when(mockApiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))
      when(mockApiHubService.removeTeamMemberFromTeam(any(), any())(any())) thenReturn Future.successful(Some(()))

      val application = fixture.playApplication
      val crypto = fixture.cryptoProvider.get()

      running(application) {
        val view = application.injector.instanceOf[RemoveTeamMemberSuccessView]
        val request = FakeRequest(POST, routes.RemoveTeamMemberController.onRemovalSubmit(team.id, crypto.encrypt(PlainText(teamMember2.email)).value).url)
          .withFormUrlEncodedBody(("value", "true"))
        val msgs: Messages = messages(application)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustBe view(team, user)(request, msgs).toString

        verify(mockApiHubService).removeTeamMemberFromTeam(matches(team.id), same(teamMember2))(any())
      }
    }

    "must redirect to the manage team page on a negative confirmation" in {
      val teamMember1 = TeamMember("creator@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")

      val user = UserModel("test-user-id", "test-user-name", LdapUser, Some(teamMember1.email))

      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(
          teamMember1,
          teamMember2
        )
      )
      
      val fixture = buildFixture(user)
      val mockApiHubService = fixture.apiHubService
      
      when(mockApiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))

      val application = fixture.playApplication
      val crypto = fixture.cryptoProvider.get()

      running(application) {
        val request = FakeRequest(POST, routes.RemoveTeamMemberController.onRemovalSubmit(team.id, crypto.encrypt(PlainText(teamMember2.email)).value).url)
          .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ManageTeamController.onPageLoad(team.id).url
      }
    }

    "must return an error on a self removal attempt" in {
      val teamMember1 = TeamMember("creator@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")

      val user = UserModel("test-user-id", "test-user-name", LdapUser, Some(teamMember1.email))

      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(
          teamMember1,
          teamMember2
        )
      )
      
      val fixture = buildFixture(user)
      val mockApiHubService = fixture.apiHubService
      
      when(mockApiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))
      when(mockApiHubService.removeTeamMemberFromTeam(any(), any())(any())) thenReturn Future.successful(Some(()))

      val application = fixture.playApplication
      val crypto = fixture.cryptoProvider.get()

      running(application) {
        val view = application.injector.instanceOf[ErrorTemplate]
        val request = FakeRequest(POST, routes.RemoveTeamMemberController.onRemovalSubmit(team.id, crypto.encrypt(PlainText(teamMember1.email)).value).url)
          .withFormUrlEncodedBody(("value", "true"))
        val msgs: Messages = messages(application)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustBe
          view(
            "Bad request - 400",
            "Bad request",
            "Cannot delete the authenticated user."
          )(request, msgs)
            .toString()

        verify(mockApiHubService, never).removeTeamMemberFromTeam(matches(team.id), same(teamMember1))(any())
      }
    }

    "must return an error if there is no removal confirmation on form submission" in {
      val teamMember1 = TeamMember("creator@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")

      val user = UserModel("test-user-id", "test-user-name", LdapUser, Some(teamMember1.email))

      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(
          teamMember1,
          teamMember2
        )
      )
      
      val fixture = buildFixture(user)
      val mockApiHubService = fixture.apiHubService
      
      when(mockApiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))
      when(mockApiHubService.removeTeamMemberFromTeam(any(), any())(any())) thenReturn Future.successful(Some(()))

      val application = fixture.playApplication
      val crypto = fixture.cryptoProvider.get()

      running(application) {
        val view = application.injector.instanceOf[RemoveTeamMemberConfirmationView]
        val request = FakeRequest(POST, routes.RemoveTeamMemberController.onRemovalSubmit(team.id, crypto.encrypt(PlainText(teamMember2.email)).value).url)
          .withFormUrlEncodedBody(("value", ""))
        val msgs: Messages = messages(application)
        val form = new YesNoFormProvider()("")
          .withError(FormError("value", "manageTeam.teamMembers.removeTeamMember.error"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustBe
          view(
            team,
            teamMember2,
            crypto.encrypt(PlainText(teamMember2.email)).value,
            user,
            form
          )(request, msgs)
            .toString()

        verify(mockApiHubService, never).removeTeamMemberFromTeam(matches(team.id), same(teamMember1))(any())
      }
    }

    "must return an error when the team member does not exist" in {
      val teamMember1 = TeamMember("creator@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")

      val user = UserModel("test-user-id", "test-user-name", LdapUser, Some(teamMember1.email))

      val team = Team(
        "test-team-id",
        "test-team-name",
        LocalDateTime.now(),
        Seq(
          teamMember1,
          teamMember2
        )
      )
      
      val fixture = buildFixture(user)
      val mockApiHubService = fixture.apiHubService
      
      when(mockApiHubService.findTeamById(any)(any)).thenReturn(Future.successful(Some(team)))
      when(mockApiHubService.removeTeamMemberFromTeam(any(), any())(any())) thenReturn Future.successful(None)

      val application = fixture.playApplication
      val crypto = fixture.cryptoProvider.get()

      running(application) {
        val view = application.injector.instanceOf[ErrorTemplate]
        val request = FakeRequest(POST, routes.RemoveTeamMemberController.onRemovalSubmit(team.id, crypto.encrypt(PlainText(teamMember2.email)).value).url)
          .withFormUrlEncodedBody(("value", "true"))
        val msgs: Messages = messages(application)

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "This page can’t be found",
            "Cannot find this team member."
          )(request, msgs)
            .toString()

        verify(mockApiHubService).removeTeamMemberFromTeam(matches(team.id), same(teamMember2))(any())
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService, cryptoProvider: CryptoProvider)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]
    val cryptoProvider = mock[CryptoProvider]

    when(cryptoProvider.get()).thenReturn(SymmetricCryptoFactory.aesCrypto("gvB1GdgzqG1AarzF1LY0zQ=="))

    val playApplication = applicationBuilder(user = userModel)
        .overrides(
          bind[ApiHubService].toInstance(apiHubService),
          bind[CryptoProvider].toInstance(cryptoProvider),
        )
        .build()

    Fixture(playApplication, apiHubService, cryptoProvider)
  }
}
