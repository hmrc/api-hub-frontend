package viewmodels.checkAnswers.myapis.produce

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.myapis.produce.ProduceApiEnterOasPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ProduceApiEnterOasSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ProduceApiEnterOasPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "produceApiEnterOas.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.ProduceApiEnterOasController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("produceApiEnterOas.change.hidden"))
          )
        )
    }
}
