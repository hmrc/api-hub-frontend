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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Call
import controllers.routes
import pages._
import models._

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ApplicationNamePage => _ => routes.QuestionAddTeamMembersController.onPageLoad(NormalMode)
    case QuestionAddTeamMembersPage => questionAddTeamMembersNextPage(NormalMode)
    case TeamMembersPage => _ => routes.ConfirmAddTeamMemberController.onPageLoad(NormalMode)
    case ConfirmAddTeamMemberPage => confirmAddTeamMemberNextPage(NormalMode)
    case AddAnApiApiIdPage => _ => routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode)
    case AddAnApiSelectApplicationPage => _ => routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode)
    case AddAnApiSelectEndpointsPage => _ => routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode)
    case ApiPolicyConditionsDeclarationPage => _ => routes.AddAnApiCheckYourAnswersController.onPageLoad()
    case _ => _ => routes.IndexController.onPageLoad
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case QuestionAddTeamMembersPage => questionAddTeamMembersNextPage(CheckMode)
    case ConfirmAddTeamMemberPage => confirmAddTeamMemberNextPage(CheckMode)
    case TeamMembersPage => _ => routes.ConfirmAddTeamMemberController.onPageLoad(CheckMode)
    case AddAnApiSelectApplicationPage => _ => routes.AddAnApiCheckYourAnswersController.onPageLoad()
    case AddAnApiSelectEndpointsPage => _ => routes.AddAnApiCheckYourAnswersController.onPageLoad()
    case ApiPolicyConditionsDeclarationPage => _ => routes.AddAnApiCheckYourAnswersController.onPageLoad()
    case _ => _ => routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }

  private def questionAddTeamMembersNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(QuestionAddTeamMembersPage)) match {
      case (NormalMode, Some(true)) => routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      case (NormalMode, Some(_)) => routes.CheckYourAnswersController.onPageLoad()
      case (NormalMode, None) => routes.JourneyRecoveryController.onPageLoad()
      case (CheckMode, Some(true)) => routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      case _ => routes.CheckYourAnswersController.onPageLoad()
    }
  }

  private def confirmAddTeamMemberNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(ConfirmAddTeamMemberPage)) match {
      case (NormalMode, Some(true)) => routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      case (NormalMode, Some(_)) => routes.CheckYourAnswersController.onPageLoad()
      case (NormalMode, None) => routes.JourneyRecoveryController.onPageLoad()
      case (CheckMode, Some(true)) => routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      case _ => routes.CheckYourAnswersController.onPageLoad()
    }
  }
}
