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

package viewmodels.team

import models.application.TeamMember
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Key, SummaryListRow}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object ManageTeamMembers {

  def rows(currentUserEmail: String, teamMembers: Seq[TeamMember])(implicit messages: Messages): Seq[SummaryListRow] = {
    val teamMembersWithIndexes = teamMembers.zipWithIndex
    val (currentUser, otherTeamMembers) = teamMembersWithIndexes
      .partition(teamMemberWithIndex => emailAddressesMatch(teamMemberWithIndex._1.email, currentUserEmail))
    val orderedTeamMembersWithIndexes = currentUser ++ otherTeamMembers.sortBy(_._1.email)

    orderedTeamMembersWithIndexes
      .map {
        zipped => SummaryListRow(
          key = Key(Text(zipped._1.email)),
          actions = actions(emailAddressesMatch(zipped._1.email, currentUserEmail), zipped._2)
        )
      }
  }

  private def actions(isCurrentUser: Boolean, index: Int)(implicit messages: Messages): Option[Actions] = {
    if (isCurrentUser) {
      None
    }
    else {
      Some(Actions(
        items = Seq(
          ActionItemViewModel(
            "site.remove",
            controllers.team.routes.RemoveTeamMemberController.removeTeamMember(index).url
          )
        )
      ))
    }
  }

  private def emailAddressesMatch(email: String, currentUserEmail: String): Boolean = email.toLowerCase == currentUserEmail.toLowerCase

}
