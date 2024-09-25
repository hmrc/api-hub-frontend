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
import pages.*
import models.*
import models.myapis.produce.ProduceApiHowToCreate.Editor
import org.scalatest.TryValues
import pages.application.accessrequest.{ProvideSupportingInformationPage, RequestProductionAccessSelectApisPage, RequestProductionAccessStartPage}
import pages.application.register.{RegisterApplicationNamePage, RegisterApplicationStartPage, RegisterApplicationTeamPage}
import pages.myapis.produce.{ProduceApiBeforeYouStartPage, ProduceApiHowToCreatePage, ProduceApiStartPage}

class NavigatorSpec extends SpecBase with TryValues {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {
      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
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
        "must go from the Application Name page to the Application Team page" in {
          navigator.nextPage(RegisterApplicationNamePage, NormalMode, emptyUserAnswers) mustBe controllers.application.register.routes.RegisterApplicationTeamController.onPageLoad(NormalMode)
        }
        "must go from the Application Team page to the Check Your Answers page" in {
          navigator.nextPage(RegisterApplicationTeamPage, NormalMode, emptyUserAnswers) mustBe controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad()
        }
      }

      "during the Request Production Access journey" - {
        "must start with the Select APIs page" in {
          navigator.nextPage(RequestProductionAccessStartPage, NormalMode, emptyUserAnswers) mustBe controllers.application.accessrequest.routes.RequestProductionAccessSelectApisController.onPageLoad(NormalMode)
        }
        "must go from the Select APIs page to the Provide supporting information page" in {
          navigator.nextPage(RequestProductionAccessSelectApisPage, NormalMode, emptyUserAnswers) mustBe controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(NormalMode)
        }
        "must go from the Provide supporting information page to the Check your answers page" in {
          navigator.nextPage(ProvideSupportingInformationPage, NormalMode, emptyUserAnswers) mustBe controllers.application.accessrequest.routes.RequestProductionAccessController.onPageLoad()
        }
      }

      "during the Produce an API journey" - {
        "must start with the Before you Start page" in {
          navigator.nextPage(ProduceApiStartPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiBeforeYouStartController.onPageLoad()
        }
        "must go from the Before You Start page to the How To Create page" in {
          navigator.nextPage(ProduceApiBeforeYouStartPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiHowToCreateController.onPageLoad(NormalMode)
        }
        "must go from the How To Create page to the Produce Api Enter OAS page" in {
          navigator.nextPage(ProduceApiHowToCreatePage, NormalMode, emptyUserAnswers.set(ProduceApiHowToCreatePage, Editor).get) mustBe controllers.myapis.produce.routes.ProduceApiEnterOasController.onPageLoad(NormalMode)
        }
      }
    }

    "in Check mode" - {
      "must go from a page that doesn't exist in the edit route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
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

      "during the Register Application journey" - {
        "must go from the Application Name page to the Check Your Answers page" in {
          navigator.nextPage(RegisterApplicationNamePage, CheckMode, emptyUserAnswers) mustBe controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad()
        }
        "must go from the Application Team page to the Check Your Answers page" in {
          navigator.nextPage(RegisterApplicationTeamPage, CheckMode, emptyUserAnswers) mustBe controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad()
        }
      }

      "during the Request Production Access journey" - {
        "must go from the Select APIs page to the Provide supporting information page" in {
          navigator.nextPage(RequestProductionAccessSelectApisPage, CheckMode, emptyUserAnswers) mustBe controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(CheckMode)
        }
        "must go from the Provide supporting information page to the Check your answers page" in {
          navigator.nextPage(ProvideSupportingInformationPage, CheckMode, emptyUserAnswers) mustBe controllers.application.accessrequest.routes.RequestProductionAccessController.onPageLoad()
        }
      }
    }
  }

  private def buildUserAnswers(context: AddAnApiContext): UserAnswers = {
    UserAnswers("id").set(AddAnApiContextPage, context).toOption.value
  }

}
