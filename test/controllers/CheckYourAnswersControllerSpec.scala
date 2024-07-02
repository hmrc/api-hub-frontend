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

package controllers

import base.SpecBase
import controllers.CheckYourAnswersControllerSpec.{buildApplicationDetailsSummaryList, buildTeamMembersSummaryList}
import controllers.actions.FakeUser
import generators.Generators
import models.UserAnswers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.HtmlValidation
import viewmodels.checkAnswers.{ApplicationNameSummary, TeamMembersSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with ScalaCheckPropertyChecks with Generators with HtmlValidation {

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET with empty user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val applicationDetails = buildApplicationDetailsSummaryList(emptyUserAnswers, messages(application))
        val teamMemberDetails = buildTeamMembersSummaryList(emptyUserAnswers, messages(application))
        val postCall = routes.RegisterApplicationController.create()

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(applicationDetails, teamMemberDetails, postCall, Some(FakeUser))(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET with complete user answers" in {

      forAll((userAnswers: UserAnswers) => {
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]
          val applicationDetails = buildApplicationDetailsSummaryList(userAnswers, messages(application))
          val teamMemberDetails = buildTeamMembersSummaryList(userAnswers, messages(application))
          val postCall = routes.RegisterApplicationController.create()

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(applicationDetails, teamMemberDetails, postCall, Some(FakeUser))(request, messages(application)).toString
          contentAsString(result) must validateAsHtml
        }
      })

    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}

object CheckYourAnswersControllerSpec extends SummaryListFluency {

  def buildApplicationDetailsSummaryList(userAnswers: UserAnswers, messages: Messages): SummaryList = {
    ApplicationNameSummary.summary(userAnswers)(messages)
  }

  def buildTeamMembersSummaryList(userAnswers: UserAnswers, messages: Messages): SummaryList = {
    TeamMembersSummary.summary(userAnswers)(messages)
  }

}
