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
import models.UserAnswers
import models.application.TeamMember
import org.scalatest.OptionValues
import pages.CreateTeamMembersPage
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.HtmlValidation
import viewmodels.team.ManageTeamMembers
import views.html.team.ManageTeamMembersView

class ManageTeamMembersControllerSpec extends SpecBase with HtmlValidation with OptionValues{
  "ManageTeamMembersController.onPageLoad" - {
    val teamMemberList = Seq(TeamMember(FakeUser.email.get), TeamMember("user1@example.com"), TeamMember("user2@example.com"))

    "must return OK and the correct view for a GET when team members exist" in {
      val fixture = buildFixture(Some(teamMemberList))

      running(fixture.application) {
        val request = FakeRequest(routes.ManageTeamMembersController.onPageLoad())
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ManageTeamMembersView]
        val msgs = messages(fixture.application)

        status(result) mustBe OK
        contentAsString(result) mustBe view(SummaryList(rows=ManageTeamMembers.rows(FakeUser.email.get, teamMemberList)(msgs)), FakeUser)(request, msgs).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must display error page if UserAnswers not populated correctly" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(routes.ManageTeamMembersController.onPageLoad())
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "must display error page if current user has no email" in {
      val fixture = buildFixture(Some(teamMemberList), None)

      running(fixture.application) {
        val request = FakeRequest(routes.ManageTeamMembersController.onPageLoad())
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

  }

  "ManageTeamMembersController.onContinue" - {
    "must redirect to the next page for a POST" in {
      val fixture = buildFixture(Some(Seq[TeamMember]()))

      running(fixture.application) {
        val request = FakeRequest(routes.ManageTeamMembersController.onContinue())
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IndexController.onPageLoad.url)
      }
    }
  }

  private case class Fixture(application: Application)

  private def buildFixture(maybeTeamMembers: Option[Seq[TeamMember]], currentUserEmail: Option[String] = FakeUser.email): Fixture = {
    val userAnswers = UserAnswers("id")
    val application = applicationBuilder(
        user = FakeUser.copy(email = currentUserEmail),
        userAnswers = maybeTeamMembers.map(userAnswers.set(CreateTeamMembersPage, _).toOption.value)
    ).build()

    Fixture(application)
  }

}
