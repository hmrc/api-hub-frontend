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

import controllers.team.routes
import models.UserAnswers
import pages.CreateTeamMembersPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Key, SummaryListRow}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CreateTeamAddTeamMemberSummary  {

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] =
    answers
      .get(CreateTeamMembersPage).getOrElse(Seq.empty)
      .zipWithIndex
      .map {
        zipped =>
          SummaryListRow(
            key = Key(Text(zipped._1.email)),
            actions = Some(Actions(
              items = Seq(ActionItemViewModel("site.change", routes.CreateTeamMemberController.onPageLoad().url)
              .withVisuallyHiddenText(messages("createTeamName.change.hidden"))))))
      }
}
