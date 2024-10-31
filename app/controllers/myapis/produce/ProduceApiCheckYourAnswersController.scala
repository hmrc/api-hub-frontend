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

package controllers.myapis.produce

import controllers.actions.*
import models.UserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.myapis.produce.{ProduceApiChooseTeamSummary, ProduceApiDomainSummary, ProduceApiEgressPrefixesSummary, ProduceApiEgressSummary, ProduceApiEnterOasSummary, ProduceApiHodSummary, ProduceApiPassthroughSummary, ProduceApiReviewNameDescriptionSummary, ProduceApiStatusSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.myapis.produce.ProduceApiCheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ProduceApiCheckYourAnswersController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             getData: ProduceApiDataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: ProduceApiCheckYourAnswersView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val summaryRows = Seq(
        ProduceApiChooseTeamSummary.row,
        ProduceApiEnterOasSummary.row,
        ProduceApiReviewNameDescriptionSummary.row,
        ProduceApiEgressSummary.row,
        ProduceApiEgressPrefixesSummary.row,
        ProduceApiHodSummary.row,
        ProduceApiDomainSummary.row,
        ProduceApiStatusSummary.row,
        ProduceApiPassthroughSummary.row
      ).flatMap(_(request.userAnswers))

      Ok(view(SummaryListViewModel(summaryRows), request.user))
  }

  def onSubmit(next: String): Action[AnyContent] = identify {
    implicit request => {
      next match {
        case "ok" => Redirect(routes.ProduceApiDeploymentController.onPageLoad())
        case "error" => Redirect(routes.ProduceApiDeploymentErrorController.onPageLoad())
      }
    }
  }
}
