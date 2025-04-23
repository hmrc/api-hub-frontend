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

import config.Domains
import controllers.myapis.produce.routes
import models.{CheckMode, UserAnswers}
import pages.myapis.produce.ProduceApiDomainPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ProduceApiDomainSummary  {

  def row(answers: UserAnswers, domains: Domains)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ProduceApiDomainPage).map {
      answer =>
        val domainLabel = messages("produceApiDomain.checkYourAnswersLabel.1")
        val domainDescription = domains.getDomainDescription(answer.domain)
        val subDomain = domains.getSubDomain(answer.domain, answer.subDomain)
        val subDomainLabel = messages("produceApiDomain.checkYourAnswersLabel.2")
        val subDomainDescription = subDomain.map(_.description).getOrElse(answer.subDomain)
        val basePathLabel = messages("produceApiDomain.checkYourAnswersLabel.3")
        val basePath = subDomain.map(_.basePath).getOrElse("")

        val value = ValueViewModel(
          HtmlContent(
            s"$domainLabel: ${domainDescription}<br>" +
            s"$subDomainLabel: ${subDomainDescription}<br>" +
            s"$basePathLabel: ${basePath}"
          )
        )

        SummaryListRowViewModel(
          key     = "produceApiDomain.checkYourAnswersLabel.1",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.ProduceApiDomainController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("produceApiDomain.change.hidden"))
          )
        )
    }
}
