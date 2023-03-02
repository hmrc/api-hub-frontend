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

package navigation

import base.SpecBase
import controllers.routes
import pages._
import models._
import org.scalatest.TryValues

class NavigatorSpec extends SpecBase with TryValues {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
      }

      "must go from the Application Name page to the Do you want to add a team member? page" in {
        navigator.nextPage(ApplicationNamePage, NormalMode, UserAnswers("id")) mustBe routes.QuestionAddTeamMembersController.onPageLoad(NormalMode)
      }

      "must go from the Do you want to add a team member? page to the Add a team member page when the user selects yes" in {
        val userAnswers = UserAnswers("id").set(QuestionAddTeamMembersPage, true).success.value
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, userAnswers) mustBe routes.AddTeamMemberDetailsController.onPageLoad(NormalMode)
      }

      "must go from the Do you want to add a team member? page to the Check Your Answers page when the user selects no" in {
        val userAnswers = UserAnswers("id").set(QuestionAddTeamMembersPage, false).success.value
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from the Do you want to add a team member? page to the Journey recovery page when there is no selection" in {
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, UserAnswers("id")) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from the Application Name page to the Check Your Answers Page" in {
        navigator.nextPage(ApplicationNamePage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad
      }
    }
  }
}
