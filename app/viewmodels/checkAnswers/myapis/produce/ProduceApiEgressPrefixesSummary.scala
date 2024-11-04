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

import models.myapis.produce.ProduceApiEgressPrefixMapping
import models.{CheckMode, UserAnswers}
import pages.myapis.produce.ProduceApiEgressPrefixesPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ProduceApiEgressPrefixesSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    val value = answers.get(ProduceApiEgressPrefixesPage) match {
      case Some(answer) if ! answer.isEmpty => buildPrefixesToRemove(answer.prefixes) + "" + buildMappings(answer.getMappings)
      case _ => messages("produceApiEgressPrefix.checkYourAnswersValue.none")
    }
    val changeUrl = answers.get(ProduceApiEgressPrefixesPage) match {
      case Some(_) => controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(CheckMode).url
      case None => controllers.myapis.produce.routes.ProduceApiEgressController.onPageLoad(CheckMode).url
    }
    Some(SummaryListRowViewModel(
      key     = "produceApiEgressPrefix.checkYourAnswersLabel",
      value   = ValueViewModel(HtmlContent(value)),
      actions = Seq(
        ActionItemViewModel("site.change", changeUrl)
          .withVisuallyHiddenText(messages("produceApiEgressPrefix.change.hidden"))
      )
    ))
    
  private def buildPrefixesToRemove(prefixes: Seq[String])(implicit messages: Messages): String = {
    prefixes match {
      case Nil => ""
      case _ => messages("produceApiEgressPrefix.checkYourAnswersValue.prefixes") + buildList(prefixes)
    }
  }
  
  private def buildMappings(mappings: Seq[ProduceApiEgressPrefixMapping])(implicit messages: Messages): String = {
    mappings match {
      case Nil => ""
      case _ => messages("produceApiEgressPrefix.checkYourAnswersValue.mappings") + 
        buildList(mappings.map(mapping => messages("produceApiEgressPrefix.checkYourAnswersValue.mapping", mapping.existing, mapping.replacement)))
    }
  }
  
  private def buildList(items: Seq[String]): String = {
    "<ul><li>" + items.map(HtmlFormat.escape(_)).mkString("</li><li>") + "</li></ul>"
  }
}
