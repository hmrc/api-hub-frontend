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

import controllers.actions.FakeApplication
import controllers.routes
import models.CheckMode
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow}

class AddAnApiSelectApplicationSummarySpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = play.api.test.Helpers.stubMessages()

  "row" - {
    "must return the correct summary list row when an application exists" in {
      val actual = AddAnApiSelectApplicationSummary.row(Some(FakeApplication))

      val expected = SummaryListRow(
        key = Key(Text("addAnApiSelectApplication.checkYourAnswersLabel")),
        value = Value(Text(FakeApplication.name)),
        actions = Some(Actions(
          items = Seq(
            ActionItem(
              href = routes.AddAnApiSelectApplicationController.onPageLoad(CheckMode).url,
              content = Text("site.change"),
              visuallyHiddenText = Some("addAnApiSelectApplication.change.hidden")
            )
          )
        ))
      )

      actual mustBe Some(expected)
    }

    "must return the correct summary list row when an application does not exist" in {
      AddAnApiSelectApplicationSummary.row(None) mustBe None
    }
  }

}
