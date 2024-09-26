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
import pages.application.accessrequest.ProvideSupportingInformationPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.{ActionItemViewModel, SummaryListRowViewModel}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ProvideSupportingInformationSummary {

  def row(userAnswers: UserAnswers)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "provideSupportingInformation.checkYourAnswersLabel",
      value = supportingInformationValue(userAnswers),
      actions = Seq(
        ActionItemViewModel(
          "site.change",
          controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(CheckMode).url
        ).withVisuallyHiddenText(messages("provideSupportingInformation.change.hidden"))
      )
    )
  }

  private def supportingInformationValue(userAnswers: UserAnswers)(implicit messages: Messages): Value = {
    userAnswers
      .get(ProvideSupportingInformationPage)
      .map(supportingInformation => Value(supportingInformation))
      .getOrElse(Value())
  }

}
