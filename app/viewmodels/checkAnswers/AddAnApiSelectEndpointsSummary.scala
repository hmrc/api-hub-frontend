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

package viewmodels.checkAnswers

import controllers.routes
import models.api.ApiDetail
import models.{AddAnApiContext, AvailableEndpoint, AvailableEndpoints, CheckMode, UserAnswers}
import pages.AddAnApiSelectEndpointsPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AddAnApiSelectEndpointsSummary  {

  def row(answers: UserAnswers, apiDetail: ApiDetail, context: AddAnApiContext)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AddAnApiSelectEndpointsPage).map {
      answers =>

        val endpoints = AvailableEndpoints.selectedEndpoints(apiDetail, answers)
          .values
          .flatten
          .map(endpoint => s"<li>${httpMethod(endpoint)} ${endpoint.path}</li>")
          .mkString

        val value = ValueViewModel(
          HtmlContent(
            s"<ul class='govuk-list'>$endpoints</ul>"
          )
        )

        SummaryListRowViewModel(
          key     = "addAnApiSelectEndpoints.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode, context).url)
              .withVisuallyHiddenText(messages("addAnApiSelectEndpoints.change.hidden"))
          )
        )
    }

  private def httpMethod(endpoint: AvailableEndpoint): String = {
    s"<strong class='govuk-tag govuk-tag--blue'>${endpoint.endpointMethod.httpMethod}</strong>"
  }

}
