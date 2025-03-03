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

package viewmodels.checkAnswers.application.accessrequest

import com.google.inject.{Inject, Singleton}
import config.HipEnvironments
import models.UserAnswers
import pages.application.accessrequest.RequestProductionAccessEnvironmentIdPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.SummaryListRowViewModel
import viewmodels.implicits.*

@Singleton
class RequestProductionAccessEnvironmentSummary @Inject()(hipEnvironments: HipEnvironments) {

  def row(userAnswers: UserAnswers)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "requestProductionAccess.environment.name",
      value = environmentNameValue(userAnswers),
      actions = Seq.empty
    )
  }

  private def environmentNameValue(userAnswers: UserAnswers)(implicit messages: Messages): Value = {
    userAnswers.get(RequestProductionAccessEnvironmentIdPage)
      .map(hipEnvironments.forId)
      .map(hipEnvironment => Value(hipEnvironment.nameKey))
      .getOrElse(Value())
  }

}
