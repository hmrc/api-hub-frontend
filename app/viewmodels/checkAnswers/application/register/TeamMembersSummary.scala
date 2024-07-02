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

package viewmodels.checkAnswers.application.register

import models.UserAnswers
import models.application.TeamMember
import pages.application.register.RegisterApplicationTeamMembersPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList, SummaryListRow, Value}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TeamMembersSummary  {

  def summary(answers: UserAnswers)(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows = rows(answers)
    ).withAttribute("data-summary-for" -> "team-members")
  }

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val teamMembers = answers.get(RegisterApplicationTeamMembersPage).getOrElse(Seq.empty)

    val summary = teamMembers.map {
      teamMember =>
        SummaryListRow(
          key = Key(Text(teamMember.email))
        )
    }

    if (teamMembers.length == 1) {
      summary :+ teamMembersCountRowOneMember()
    }
    else {
      teamMembersCountRowManyMembers(teamMembers) +: summary
    }
  }

  private def teamMembersCountRowOneMember()(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = Key(),
      value = Value(Text(messages.apply("checkYourAnswers.teamMembers.noTeamMembersAdded"))),
      actions = Seq.empty
    )

  private def teamMembersCountRowManyMembers(teamMembers: Seq[TeamMember])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "checkYourAnswers.teamMembers.teamMembersAdded",
      value = Value(Text(teamMembers.length.toString)),
      actions = Seq.empty
    )

}
