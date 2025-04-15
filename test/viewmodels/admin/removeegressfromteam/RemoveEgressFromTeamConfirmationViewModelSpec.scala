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

import controllers.actions.FakeUser
import fakes.FakeTeam
import models.api.EgressGateway
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}

class RemoveEgressFromTeamConfirmationViewModelSpec extends AnyFreeSpec with Matchers {

  import RemoveEgressFromTeamConfirmationViewModelSpec.*

  "RemoveEgressFromTeamConfirmationViewModel" - {
    "must return the correct summaries" in {
      val actual = RemoveEgressFromTeamConfirmationViewModel(FakeTeam, egress, FakeUser).summaries
      val expected = Seq(
        SummaryListRow(
          key = Key(content = Text("removeEgressFromTeamConfirmation.summary.egress.friendlyName")),
          value = Value(content = HtmlContent(egress.friendlyName))
        ),
        SummaryListRow(
          key = Key(content = Text("removeEgressFromTeamConfirmation.summary.egress.id")),
          value = Value(content = HtmlContent(egress.id))
        ),
        SummaryListRow(
          key = Key(content = Text("removeEgressFromTeamConfirmation.summary.team.name")),
          value = Value(content = HtmlContent(FakeTeam.name))
        )
      )

      actual must contain theSameElementsInOrderAs expected
    }

    "must return the correct submitRoute" in {
      val actual = RemoveEgressFromTeamConfirmationViewModel(FakeTeam, egress, FakeUser).submitRoute
      val expected = controllers.admin.removeegressfromteam.routes.RemoveEgressFromTeamConfirmationController.onSubmit(FakeTeam.id, egress.id)

      actual mustBe expected
    }

    "must return the correct cancelUrl" in {
      val actual = RemoveEgressFromTeamConfirmationViewModel(FakeTeam, egress, FakeUser).cancelUrl
      val expected = controllers.team.routes.ManageTeamEgressesController.onPageLoad(FakeTeam.id).url

      actual mustBe expected
    }
  }

}

private object RemoveEgressFromTeamConfirmationViewModelSpec {

  implicit val messages: Messages = Helpers.stubMessages()

  val egress: EgressGateway = EgressGateway(
    id = "test-egress-id",
    friendlyName = "test-friendly-name"
  )

}
