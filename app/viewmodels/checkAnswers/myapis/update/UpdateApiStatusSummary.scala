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

package viewmodels.checkAnswers.myapis.update

import config.ApiStatuses
import controllers.myapis.update.routes
import models.user.UserModel
import models.{CheckMode, UserAnswers}
import pages.myapis.update.UpdateApiStatusPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object UpdateApiStatusSummary  {

  def row(answers: UserAnswers, user: UserModel, apiStatuses: ApiStatuses)(implicit messages: Messages): Option[SummaryListRow] =
      answers.get(UpdateApiStatusPage).filter(_ => user.permissions.canSupport).map {
        apiStatus =>
          val value = ValueViewModel(
            HtmlContent(apiStatuses.description(apiStatus))
          )

          SummaryListRowViewModel(
            key     = "produceApiStatus.checkYourAnswersLabel",
            value   = value,
            actions = Seq(
              ActionItemViewModel("site.change", routes.UpdateApiStatusController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("produceApiStatus.change.hidden"))
            )
          )
      }
}
