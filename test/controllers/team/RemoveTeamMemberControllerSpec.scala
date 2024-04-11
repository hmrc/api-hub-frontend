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
import models.UserAnswers
import models.application.TeamMember
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.CreateTeamMembersPage
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.CreateTeamSessionRepository
import utils.HtmlValidation
import views.html.ErrorTemplate

import scala.concurrent.Future

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
        redirectLocation(result).value mustEqual controllers.routes.IndexController.onPageLoad.url

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
  }
}
