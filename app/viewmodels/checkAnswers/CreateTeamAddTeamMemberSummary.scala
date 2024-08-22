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

package viewmodels.checkAnswers

import models.UserAnswers
import pages.CreateTeamMembersPage
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList, SummaryListRow}
import viewmodels.govuk.all.{FluentSummaryList, SummaryListViewModel}

object CreateTeamAddTeamMemberSummary  {

  def summary(answers: UserAnswers): SummaryList = {
    SummaryListViewModel(
      rows = rows(answers)
    ).withAttribute("data-summary-for" -> "team-members")
  }

  def rows(answers: UserAnswers): Seq[SummaryListRow] =
    answers
      .get(CreateTeamMembersPage).getOrElse(Seq.empty)
      .sortBy(_.email)
      .zipWithIndex
      .map {
        zipped =>
          SummaryListRow(
            key = Key(Text(zipped._1.email))
          )
      }

}
