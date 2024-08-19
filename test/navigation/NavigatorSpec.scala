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
import pages.application.register.RegisterApplicationStartPage

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
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, userAnswers) mustBe routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      }

      "must go from the Do you want to add a team member? page to the Check Your Answers page when the user selects no" in {
        val userAnswers = UserAnswers("id").set(QuestionAddTeamMembersPage, false).success.value
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from the Do you want to add a team member? page to the Journey recovery page when there is no selection" in {
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, UserAnswers("id")) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from the Team member details page to the Confirm add team member page" in {
        navigator.nextPage(TeamMembersPage, NormalMode, UserAnswers("id")) mustBe routes.ConfirmAddTeamMemberController.onPageLoad(NormalMode)
      }

      "must go from the Confirm Add Team Member page to the Add a team member page when option form's value = true" in {
        val userAnswers = UserAnswers("id").set(ConfirmAddTeamMemberPage, true).success.value
        navigator.nextPage(ConfirmAddTeamMemberPage, NormalMode, userAnswers) mustBe routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      }

      "must go from the Confirm Add Team Member page to the Check Your Answers page when option form's value = false" in {
        val userAnswers = UserAnswers("id").set(ConfirmAddTeamMemberPage, false).success.value
        navigator.nextPage(ConfirmAddTeamMemberPage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from the the Confirm Add Team Member page to the Journey recovery page when there is no selection" in {
        navigator.nextPage(QuestionAddTeamMembersPage, NormalMode, UserAnswers("id")) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "during the Add an API journey" - {
        "must go from the Add An API API Id (start) page to the Select Application page" in {
          navigator.nextPage(AddAnApiApiPage, NormalMode, buildUserAnswers(AddAnApi)) mustBe routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode)
        }

        "must go from the Add An API Select Application page to the Select Endpoints page" in {
          navigator.nextPage(AddAnApiSelectApplicationPage, NormalMode, buildUserAnswers(AddAnApi)) mustBe routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode, AddAnApi)
        }

        "must go from the Add An API Select Endpoints page to the Accept Policy Conditions page" in {
          navigator.nextPage(AddAnApiSelectEndpointsPage, NormalMode, buildUserAnswers(AddAnApi)) mustBe routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode, AddAnApi)
        }

        "must go from the Add An API Accept Policy Conditions page to the Check Your Answers page" in {
          navigator.nextPage(ApiPolicyConditionsDeclarationPage, NormalMode, buildUserAnswers(AddAnApi)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddAnApi)
        }
      }

      "during the Add Endpoints journey" - {
        "must go from the Add An API API Id (start) page to the Select Endpoints page" in {
          navigator.nextPage(AddAnApiApiPage, NormalMode, buildUserAnswers(AddEndpoints)) mustBe routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode, AddEndpoints)
        }

        "must go from the Add An API Select Endpoints page to the Accept Policy Conditions page" in {
          navigator.nextPage(AddAnApiSelectEndpointsPage, NormalMode, buildUserAnswers(AddEndpoints)) mustBe routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode, AddEndpoints)
        }

        "must go from the Add An API Accept Policy Conditions page to the Check Your Answers page" in {
          navigator.nextPage(ApiPolicyConditionsDeclarationPage, NormalMode, buildUserAnswers(AddEndpoints)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddEndpoints)
        }
      }

      "during the Create Team journey" - {
        "must start with the Team Name page" in {
          navigator.nextPage(CreateTeamStartPage, NormalMode, emptyUserAnswers) mustBe controllers.team.routes.CreateTeamNameController.onPageLoad(NormalMode)
        }

        "must go from the Team Name page to the Team Members page" in {
          navigator.nextPage(CreateTeamNamePage, NormalMode, emptyUserAnswers) mustBe controllers.team.routes.ManageTeamMembersController.onPageLoad()
        }

        "must go from the Team Members page to the Check Your Answers page" in {
          navigator.nextPage(CreateTeamMembersPage, NormalMode, emptyUserAnswers) mustBe controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad()
        }

        "must go from the Team Member page to the Team Members page" in {
          navigator.nextPage(CreateTeamMemberPage, NormalMode, emptyUserAnswers) mustBe controllers.team.routes.ManageTeamMembersController.onPageLoad()
        }
      }

      "during the Register Application journey" - {
        "must start with the Application Name page" in {
          navigator.nextPage(RegisterApplicationStartPage, NormalMode, emptyUserAnswers) mustBe controllers.application.register.routes.RegisterApplicationNameController.onPageLoad(NormalMode)
        }
      }
    }

    "in Check mode" - {
      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from the Application Name page to the Check Your Answers Page" in {
        navigator.nextPage(ApplicationNamePage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from the Do you want to add a team member? page to the Add a team member page when the user selects yes" in {
        val userAnswers = UserAnswers("id").set(QuestionAddTeamMembersPage, true).success.value
        navigator.nextPage(QuestionAddTeamMembersPage, CheckMode, userAnswers) mustBe routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0)
      }

      "must go from the Do you need to add another team member? on Confirm Add Team Member page to the Check Your Answers page when the user selects no" in {
        val userAnswers = UserAnswers("id").set(ConfirmAddTeamMemberPage, false).success.value
        navigator.nextPage(ConfirmAddTeamMemberPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from the Team member details page to the Confirm add team member page" in {
        navigator.nextPage(TeamMembersPage, CheckMode, UserAnswers("id")) mustBe routes.ConfirmAddTeamMemberController.onPageLoad(CheckMode)
      }

      "during the Add An Api journey" - {
        "must go from the Add An API Select Application page to the Check Your Answers page" in {
          navigator.nextPage(AddAnApiSelectApplicationPage, CheckMode, buildUserAnswers(AddAnApi)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddAnApi)
        }

        "must go from the Add An API Select Endpoints page to the Check Your Answers page" in {
          navigator.nextPage(AddAnApiSelectEndpointsPage, CheckMode, buildUserAnswers(AddAnApi)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddAnApi)
        }

        "must go from the Add An API Accept Policy Conditions page to the Check Your Answers page" in {
          navigator.nextPage(ApiPolicyConditionsDeclarationPage, CheckMode, buildUserAnswers(AddAnApi)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddAnApi)
        }
      }

      "during the Add Endpoints journey" - {
        "must go from the Add An API Select Endpoints page to the Check Your Answers page" in {
          navigator.nextPage(AddAnApiSelectEndpointsPage, CheckMode, buildUserAnswers(AddEndpoints)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddEndpoints)
        }

        "must go from the Add An API Accept Policy Conditions page to the Check Your Answers page" in {
          navigator.nextPage(ApiPolicyConditionsDeclarationPage, CheckMode, buildUserAnswers(AddEndpoints)) mustBe routes.AddAnApiCheckYourAnswersController.onPageLoad(AddEndpoints)
        }
      }

      "during the Create Team journey" - {
        "must go from Team Name to Check Your Answers" in {
          navigator.nextPage(CreateTeamNamePage, CheckMode, emptyUserAnswers) mustBe controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad()
        }
      }
    }
  }

  private def buildUserAnswers(context: AddAnApiContext): UserAnswers = {
    UserAnswers("id").set(AddAnApiContextPage, context).toOption.value
  }

}
