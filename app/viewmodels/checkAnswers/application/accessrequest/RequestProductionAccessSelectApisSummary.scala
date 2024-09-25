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

import controllers.application.routes
import models.{CheckMode, UserAnswers}
import pages.application.accessrequest.{RequestProductionAccessApisPage, RequestProductionAccessSelectApisPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.application.ApplicationApi
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object RequestProductionAccessSelectApisSummary  {

  def row(userAnswers: UserAnswers)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "requestProductionAccessSelectApis.checkYourAnswersLabel",
      value   = selectedApisValue(userAnswers),
      actions = Seq(
        ActionItemViewModel("site.change", routes.RequestProductionAccessSelectApisController.onPageLoad(CheckMode).url)
          .withVisuallyHiddenText(messages("requestProductionAccessSelectApis.change.hidden"))
      )
    )
  }

  def buildSelectedApis(userAnswers: UserAnswers)(implicit messages: Messages): Seq[ApplicationApi] = {
    applicationApis(userAnswers)
      .filter(api => selectedApis(userAnswers).exists(_.equals(api.apiId)))
  }

  private def selectedApisValue(userAnswers: UserAnswers)(implicit messages: Messages) = {
    val listItems = buildSelectedApis(userAnswers)
      .map(api => s"<li>${api.apiTitle}</li>")
      .mkString

    Value(HtmlContent(s"<ul class='govuk-list govuk-list--bullet'>$listItems</ul>"))
  }

  private def applicationApis(userAnswers: UserAnswers): Seq[ApplicationApi] = {
    userAnswers
      .get(RequestProductionAccessApisPage)
      .getOrElse(Seq.empty)
  }

  private def selectedApis(userAnswers: UserAnswers): Set[String] = {
    userAnswers
      .get(RequestProductionAccessSelectApisPage)
      .getOrElse(Set.empty)
  }

}
