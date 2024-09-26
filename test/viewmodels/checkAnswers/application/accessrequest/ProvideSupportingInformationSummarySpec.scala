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

package viewmodels.checkAnswers.application.accessrequest

import models.{CheckMode, UserAnswers}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.application.accessrequest.ProvideSupportingInformationPage
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Value, *}

class ProvideSupportingInformationSummarySpec extends AnyFreeSpec with Matchers with OptionValues {

  import ProvideSupportingInformationSummarySpec.*

  "ProvideSupportingInformationSummary" - {
    "must provide the correct summary when the answer exists" in {
      val supportingInformation = "test-supporting-information"
      val userAnswers = UserAnswers("test-id").set(ProvideSupportingInformationPage, supportingInformation).toOption.value
      val actual = ProvideSupportingInformationSummary.row(userAnswers)
      val expected = buildSummary(Some(supportingInformation))

      actual mustBe expected
    }

    "must provide the correct summary when there is no answer" in {
      val userAnswers = UserAnswers("test-id")
      val actual = ProvideSupportingInformationSummary.row(userAnswers)
      val expected = buildSummary(None)

      actual mustBe expected
    }
  }

}

object ProvideSupportingInformationSummarySpec {

  private implicit val messages: Messages = Helpers.stubMessages()

  private def buildSummary(supportingInformation: Option[String]): SummaryListRow = {
    val value = supportingInformation
      .map(text => Value(Text(text)))
      .getOrElse(Value())

    SummaryListRow(
      key = Key(content = Text("provideSupportingInformation.checkYourAnswersLabel")),
      value = value,
      actions = Some(
        Actions(
          items = Seq(
            ActionItem(
              href = controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(CheckMode).url,
              content = Text(messages("site.change")),
              visuallyHiddenText = Some(messages("provideSupportingInformation.change.hidden"))
            )
          )
        )
      )
    )
  }

}
