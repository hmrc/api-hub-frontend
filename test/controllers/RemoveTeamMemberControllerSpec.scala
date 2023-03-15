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

package controllers

import base.SpecBase
import forms.AddTeamMemberDetailsFormProvider
import models.application.TeamMember
import models.{NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.TeamMembersPage
import play.api.test.FakeRequest
import play.api.test.Helpers._

class RemoveTeamMemberControllerSpec extends SpecBase with MockitoSugar with OptionValues with TryValues {

  private val formProvider = new AddTeamMemberDetailsFormProvider()

  "RemoveTeamMemberDetails Controller" - {

    "must return redirect to confirm team members page when successful removal of team member" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(TeamMember("creator.email@hmrc.gov.uk"), TeamMember("team.member.email@hmrc.gov.uk")))
        .success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()


      running(application) {
        val request = FakeRequest(GET, routes.RemoveTeamMemberController.removeTeamMember(1).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ConfirmAddTeamMemberController.onPageLoad(NormalMode).url
      }
    }
  }

  "must return not found when removal of creator team member" in {

    val userAnswers = UserAnswers(userAnswersId)
      .set(TeamMembersPage, Seq(TeamMember("creator.email@hmrc.gov.uk"), TeamMember("team.member.email@hmrc.gov.uk")))
      .success.value

    val application = applicationBuilder(userAnswers = Some(userAnswers)).build()


    running(application) {
      val request = FakeRequest(DELETE, routes.RemoveTeamMemberController.removeTeamMember(0).url)

      val result = route(application, request).value
      status(result) mustEqual NOT_FOUND
    }
  }

  "must return not found when removal of non-existent team member" in {

    val userAnswers = UserAnswers(userAnswersId)
      .set(TeamMembersPage, Seq(TeamMember("creator.email@hmrc.gov.uk"), TeamMember("team.member.email@hmrc.gov.uk")))
      .success.value

    val application = applicationBuilder(userAnswers = Some(userAnswers)).build()


    running(application) {
      val request = FakeRequest(DELETE, routes.RemoveTeamMemberController.removeTeamMember(99).url)

      val result = route(application, request).value
      status(result) mustEqual NOT_FOUND
    }
  }

  "must return not found when no team members user answers" in {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

    running(application) {
      val request = FakeRequest(DELETE, routes.RemoveTeamMemberController.removeTeamMember(1).url)

      val result = route(application, request).value
      status(result) mustEqual NOT_FOUND
    }
  }
}
