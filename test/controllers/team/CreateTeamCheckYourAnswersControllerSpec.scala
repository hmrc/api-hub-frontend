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

package controllers.team

import base.SpecBase
import controllers.actions.FakeUser
import controllers.team.CreateTeamCheckYourAnswersControllerSpec.{buildTeamDetailsSummaryList, buildTeamMembersSummaryList}
import generators.Generators
import models.UserAnswers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.HtmlValidation
import viewmodels.checkAnswers.{CreateTeamAddTeamMemberSummary, CreateTeamNameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.team.CreateTeamCheckYourAnswersView
class CreateTeamCheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with ScalaCheckPropertyChecks with Generators with HtmlValidation {

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET with empty user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CreateTeamCheckYourAnswersView]
        val teamDetails = buildTeamDetailsSummaryList(emptyUserAnswers, messages(application))
        val teamMemberDetails = buildTeamMembersSummaryList(emptyUserAnswers)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(teamDetails, teamMemberDetails, Some(FakeUser))(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET with complete user answers" in {

      forAll((userAnswers: UserAnswers) => {
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CreateTeamCheckYourAnswersView]
          val teamDetails = buildTeamDetailsSummaryList(userAnswers, messages(application))
          val teamMemberDetails = buildTeamMembersSummaryList(userAnswers)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(teamDetails, teamMemberDetails, Some(FakeUser))(request, messages(application)).toString
          contentAsString(result) must validateAsHtml
        }
      })

    }
  }
}

object CreateTeamCheckYourAnswersControllerSpec extends SummaryListFluency {

  def buildTeamDetailsSummaryList(userAnswers: UserAnswers, messages: Messages): SummaryList = {
    SummaryListViewModel(
      Seq(
        CreateTeamNameSummary.row(userAnswers)(messages)
      ).flatten
    )
  }

  def buildTeamMembersSummaryList(userAnswers: UserAnswers): SummaryList = {
    SummaryListViewModel(
      rows = CreateTeamAddTeamMemberSummary.rows(userAnswers)
    )
  }

}
