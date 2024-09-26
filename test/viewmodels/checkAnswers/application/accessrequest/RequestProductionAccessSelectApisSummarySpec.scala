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
import pages.application.accessrequest.{RequestProductionAccessApisPage, RequestProductionAccessSelectApisPage}
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow}
import viewmodels.application.ApplicationApi

class RequestProductionAccessSelectApisSummarySpec extends AnyFreeSpec with Matchers with OptionValues {

  import RequestProductionAccessSelectApisSummarySpec.*

  "RequestProductionAccessSelectApisSummary" - {
    "must provide the correct summary when the answer exists" in {
      val userAnswers = UserAnswers("test-id")
        .set(RequestProductionAccessApisPage, Seq(applicationApi1, applicationApi2)).toOption.value
        .set(RequestProductionAccessSelectApisPage, Set(applicationApi1.apiId)).toOption.value
      val actual = RequestProductionAccessSelectApisSummary.row(userAnswers)
      val expected = buildSummary(Some(Seq(applicationApi1)))

      actual mustBe expected
    }

    "must provide the correct summary when there is no answer" in {
      val userAnswers = UserAnswers("test-id")
      val actual = RequestProductionAccessSelectApisSummary.row(userAnswers)
      val expected = buildSummary(None)

      actual mustBe expected
    }
  }

}

object RequestProductionAccessSelectApisSummarySpec {

  private implicit val messages: Messages = Helpers.stubMessages()

  private val applicationApi1 = ApplicationApi(
    apiId = "test-id-1",
    apiTitle = "test-title-1",
    totalEndpoints = 0,
    endpoints = Seq.empty,
    hasPendingAccessRequest = false,
    isMissing = false
  )

  private val applicationApi2 = ApplicationApi(
    apiId = "test-id-2",
    apiTitle = "test-title-2",
    totalEndpoints = 0,
    endpoints = Seq.empty,
    hasPendingAccessRequest = false,
    isMissing = false
  )

  private def buildSummary(selectedApis: Option[Seq[ApplicationApi]]): SummaryListRow = {
    val listItems = selectedApis
      .map(
        apis =>
          apis.map(api => s"<li>${api.apiTitle}</li>")
      )
      .getOrElse(Seq.empty)
      .mkString

    val value = s"<ul class='govuk-list govuk-list--bullet'>$listItems</ul>"

    SummaryListRow(
      key = Key(Text("requestProductionAccessSelectApis.checkYourAnswersLabel")),
      value = Value(HtmlContent(value)),
      actions = Some(
        Actions(
          items = Seq(
            ActionItem(
              href = controllers.application.accessrequest.routes.RequestProductionAccessSelectApisController.onPageLoad(CheckMode).url,
              content = Text(messages("site.change")),
              visuallyHiddenText = Some(messages("requestProductionAccessSelectApis.change.hidden"))
            )
          )
        )
      )
    )
  }

}
