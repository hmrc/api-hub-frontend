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

package viewmodels.team

import base.SpecBase
import models.application.TeamMember
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Empty, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Key, SummaryListRow, Value}
import viewmodels.govuk.all.ActionItemViewModel

class ManageTeamMembersSpec extends SpecBase with Matchers {
  implicit val messages: Messages = play.api.test.Helpers.stubMessages()

  val currentUserEmail = "user@hmrc.gov.uk"

  "rows" - {
    "must return the correct summary rows when there is only one team member" in {
      val teamMember = TeamMember(currentUserEmail)

      val actual = ManageTeamMembers.rows(currentUserEmail, Seq(teamMember))

      val expected = Seq(
        SummaryListRow(
          key = Key(Text(teamMember.email)),
          value = Value(Empty)
        )
      )

      actual mustBe expected
    }

    "must return the correct summary rows when there are multiple team members" in {
      val teamMember1 = TeamMember("newMember2@example.com")
      val teamMember2 = TeamMember(currentUserEmail)
      val teamMember3 = TeamMember("newMember1@example.com")

      val actual = ManageTeamMembers.rows(currentUserEmail, Seq(teamMember1, teamMember2, teamMember3))

      val expected = Seq(
        SummaryListRow(
          key = Key(Text(teamMember2.email)),
          value = Value(Empty),
        ),
        SummaryListRow(
          key = Key(Text(teamMember3.email)),
          value = Value(Empty),
          actions = Some(Actions(
            items = Seq(
              ActionItemViewModel(
                Text("site.remove"),
                controllers.team.routes.RemoveTeamMemberController.removeTeamMember(2).url
              )
            )
          ))
        ),
        SummaryListRow(
          key = Key(Text(teamMember1.email)),
          value = Value(Empty),
          actions = Some(Actions(
            items = Seq(
              ActionItemViewModel(
                Text("site.remove"),
                controllers.team.routes.RemoveTeamMemberController.removeTeamMember(0).url
              )
            )
          ))
        )
      )

      actual mustBe expected
    }
  }
}