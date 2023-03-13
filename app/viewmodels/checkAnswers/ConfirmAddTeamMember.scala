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
import models.{CheckMode, UserAnswers}
import pages.TeamMembersPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Key, SummaryListRow}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object ConfirmAddTeamMember {

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] =
    answers
      .get(TeamMembersPage).getOrElse(Seq.empty)
      .zipWithIndex
      .map {
        zipped => SummaryListRow(
          key = Key(Text(zipped._1.email)),
          actions = actions(zipped._2)
        )
      }

  private def actions(index: Int)(implicit messages: Messages): Option[Actions] = {
    if (index == 0) {
      None
    }
    else {
      Some(Actions(
        items = Seq(
          ActionItemViewModel(
            "site.change",
            routes.AddTeamMemberDetailsController.onPageLoad(CheckMode, index).url
          )
        )
      ))
    }
  }

}
