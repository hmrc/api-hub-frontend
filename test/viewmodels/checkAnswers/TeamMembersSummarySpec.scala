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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.routes
import models.{CheckMode, UserAnswers}
import models.application.TeamMember
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.matchers.must.Matchers
import pages.TeamMembersPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow, Value}

class TeamMembersSummarySpec extends SpecBase with Matchers with TryValues with OptionValues {

  "rows" - {
    "must return the correct summary rows when there is only one team member" in {
      val teamMember = TeamMember("test@email.com")

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(teamMember))
        .success
        .value

      implicit val messages: Messages = play.api.test.Helpers.stubMessages()

      val actual = TeamMembersSummary.rows(userAnswers)

      val expected = Seq(
        SummaryListRow(key = Key(Text(teamMember.email))),
        SummaryListRow(
          value = Value(Text("checkYourAnswers.teamMembers.noTeamMembersAdded")),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                href = routes.QuestionAddTeamMembersController.onPageLoad(CheckMode).url,
                content = Text("site.change")
              )
            )
          ))
        )
      )

      actual mustBe expected
    }

    "must return the correct summary rows when there is more than one team member" in {
      val teamMember1 = TeamMember("test1@email.com")
      val teamMember2 = TeamMember("test2@email.com")

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(teamMember1, teamMember2))
        .success
        .value

      implicit val messages: Messages = play.api.test.Helpers.stubMessages()

      val actual = TeamMembersSummary.rows(userAnswers)

      val expected = Seq(
        SummaryListRow(
          key = Key(Text("checkYourAnswers.teamMembers.teamMembersAdded")),
          value = Value(Text("2")),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                href = routes.ConfirmAddTeamMemberController.onPageLoad(CheckMode).url,
                content = Text("site.change")
              )
            )
          ))
        ),
        SummaryListRow(key = Key(Text(teamMember1.email))),
        SummaryListRow(key = Key(Text(teamMember2.email)))
      )

      actual mustBe expected
    }
  }

}
