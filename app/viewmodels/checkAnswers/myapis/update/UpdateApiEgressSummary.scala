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

package viewmodels.checkAnswers.myapis.update

import controllers.myapis.update.routes
import models.{CheckMode, UserAnswers}
import pages.myapis.update.UpdateApiSelectEgressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object UpdateApiEgressSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    val egressValue = answers.get(UpdateApiSelectEgressPage) match {
      case Some(egress) if egress.nonEmpty => egress
      case _ => messages("myApis.environment.noEgressSelected")
    }
    Some(SummaryListRowViewModel(
      key = "myApis.produce.selectegress.cya.label",
      value = ValueViewModel(
        HtmlContent(HtmlFormat.escape(egressValue))
      ),
      actions = Seq(
        ActionItemViewModel("site.change", routes.UpdateApiSelectEgressController.onPageLoad(CheckMode).url)
          .withVisuallyHiddenText(messages("updateApiEgressSelection.change.hidden"))
      )
    ))
}