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

import controllers.routes
import models.application.TeamMember
import models.{CheckMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.TeamMembersPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow}

class ConfirmAddTeamMemberSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  implicit val messages: Messages = play.api.test.Helpers.stubMessages()

  "rows" - {
    "must return the correct summary list rows with the first row unmodifiable" in {
      val teamMember1 = TeamMember("test1@email.com")
      val teamMember2 = TeamMember("test2@email.com")
      val teamMember3 = TeamMember("test3@email.com")

      val userAnswers = UserAnswers("test-id")
        .set(TeamMembersPage, Seq(teamMember1, teamMember2, teamMember3))
        .success
        .value

      val actual = ConfirmAddTeamMember.rows(userAnswers)

      val expected = Seq(
        SummaryListRow(key = Key(Text(teamMember1.email))),
        buildModifiableRow(teamMember2, 1),
        buildModifiableRow(teamMember3, 2)
      )

      actual mustBe expected
    }
  }

  private def buildModifiableRow(teamMember: TeamMember, index: Int): SummaryListRow = {
    SummaryListRow(
      key = Key(Text(teamMember.email)),
      actions = Some(Actions(
        items = Seq(
          ActionItem(
            href = routes.AddTeamMemberDetailsController.onPageLoad(CheckMode, index).url,
            content = Text("site.change")
          )
        )
      ))
    )
  }

}
