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

package viewmodels.checkAnswers.application.register

import models.{CheckMode, UserAnswers}
import pages.application.register.RegisterApplicationNamePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object RegisterApplicationNameSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegisterApplicationNamePage).map {
      answer =>
        SummaryListRowViewModel(
          key     = "registerApplicationName.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(HtmlFormat.escape(answer).toString)),
          actions = Seq(
            ActionItemViewModel("site.change", controllers.application.register.routes.RegisterApplicationNameController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("registerApplicationName.change.hidden"))
          )
        )
    }

}
