/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.admin.removeegressfromteam

import models.api.EgressGateway
import models.team.Team
import models.user.UserModel
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

case class RemoveEgressFromTeamConfirmationViewModel(
  team: Team,
  egress: EgressGateway,
  user: UserModel
) {

  def summaries(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      SummaryListRowViewModel(
        key = "removeEgressFromTeamConfirmation.summary.egress.friendlyName",
        value = ValueViewModel(HtmlContent(egress.friendlyName))
      ),
      SummaryListRowViewModel(
        key = "removeEgressFromTeamConfirmation.summary.egress.id",
        value = ValueViewModel(HtmlContent(egress.id))
      ),
      SummaryListRowViewModel(
        key = "removeEgressFromTeamConfirmation.summary.team.name",
        value = ValueViewModel(HtmlContent(team.name))
      )
    )
  }

  def submitRoute: Call = {
    controllers.admin.removeegressfromteam.routes.RemoveEgressFromTeamConfirmationController.onSubmit(
      teamId = team.id,
      egressId = egress.id
    )
  }

  def cancelUrl: String = {
    controllers.team.routes.ManageTeamEgressesController.onPageLoad(team.id).url
  }

}
