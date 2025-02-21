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

import generators.ApiDetailGenerators
import models.api.ApiDetail
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

class AddAnApiApiIdSummarySpec extends AnyFreeSpec with Matchers with ApiDetailGenerators with ScalaCheckDrivenPropertyChecks {

  private implicit val messages: Messages = play.api.test.Helpers.stubMessages()

  "row" - {
    "must return the correct summary list row when an API exists" in {
      forAll { (apiDetail: ApiDetail) =>
        val actual = AddAnApiApiIdSummary.row(Some(apiDetail))

        val expected = SummaryListRow(
          key = Key(Text("addAnApiApiId.checkYourAnswersLabel")),
          value = Value(HtmlContent(HtmlFormat.escape(apiDetail.title).toString)),
          actions = None
        )

        actual mustBe Some(expected)
      }
    }

    "must return the correct summary list row when an API does not exist" in {
      AddAnApiApiIdSummary.row(None) mustBe None
    }
  }

}
