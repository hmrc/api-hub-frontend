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

package viewmodels.checkAnswers.myapis.produce

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.myapis.produce.ProduceApiEgressPrefixesPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import pages.myapis.produce.ProduceApiEgressPrefixesPage

object ProduceApiEgressPrefixesSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ProduceApiEgressPrefixesPage).map {
      answer =>

      val value = HtmlFormat.escape(answer.prefixes.mkString(",")).toString + "<br/>" + HtmlFormat.escape(answer.getMappings.map(_.toString).mkString(",")).toString

        SummaryListRowViewModel(
          key     = "produceApiEgressPrefix.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("produceApiEgressPrefix.change.hidden"))
          )
        )
    }
}