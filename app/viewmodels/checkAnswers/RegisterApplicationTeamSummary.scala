package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.RegisterApplicationTeamPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object RegisterApplicationTeamSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegisterApplicationTeamPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"registerApplicationTeam.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "registerApplicationTeam.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.RegisterApplicationTeamController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("registerApplicationTeam.change.hidden"))
          )
        )
    }
}
