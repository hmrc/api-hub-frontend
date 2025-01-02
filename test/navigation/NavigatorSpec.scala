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
import controllers.actions.FakeUser
import controllers.routes
import pages.*
import models.*
import models.myapis.produce.{ProduceApiChooseEgress, ProduceApiHowToAddWiremock}
import models.myapis.produce.ProduceApiHowToCreate.Editor
import models.user.Permissions
import org.scalatest.TryValues
import pages.application.accessrequest.{ProvideSupportingInformationPage, RequestProductionAccessPage, RequestProductionAccessSelectApisPage, RequestProductionAccessStartPage}
import pages.application.cancelaccessrequest.CancelAccessRequestStartPage
import pages.application.register.{RegisterApplicationNamePage, RegisterApplicationStartPage, RegisterApplicationTeamPage}
import pages.myapis.produce.*
import pages.myapis.update.*

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
        "must go from the Check your answers page to the end journey controller" in {
          navigator.nextPage(RequestProductionAccessPage, NormalMode, emptyUserAnswers) mustBe controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest()
        }
      }

      "during the Produce an API journey" - {
        "must start with the Before you Start page" in {
          navigator.nextPage(ProduceApiStartPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiBeforeYouStartController.onPageLoad()
        }
        "must go from the Before You Start page to the Choose owning team page" in {
          navigator.nextPage(ProduceApiBeforeYouStartPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiChooseTeamController.onPageLoad(NormalMode)
        }
        "must go from the Before You Start page to the How To Create page" in {
          navigator.nextPage(ProduceApiChooseTeamPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiHowToCreateController.onPageLoad(NormalMode)
        }
        "must go from the How To Create page to the Produce Api Enter OAS page" in {
          navigator.nextPage(ProduceApiHowToCreatePage, NormalMode, emptyUserAnswers.set(ProduceApiHowToCreatePage, Editor).get) mustBe controllers.myapis.produce.routes.ProduceApiEnterOasController.onPageLoad(NormalMode)
        }
        "must go from the Produce Api Enter OAS page to the Enter a short description page" in {
          navigator.nextPage(ProduceApiEnterOasPage, NormalMode, emptyUserAnswers.set(ProduceApiEnterOasPage, "oas").get) mustBe controllers.myapis.produce.routes.ProduceApiShortDescriptionController.onPageLoad(NormalMode)
        }
        "must go from the Enter a short description page to the API review page" in {
          navigator.nextPage(ProduceApiShortDescriptionPage, NormalMode, emptyUserAnswers.set(ProduceApiShortDescriptionPage, "short description").get) mustBe controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onPageLoad(NormalMode)
        }
        // TODO: bring back with Wiremock
        //        "must go from the API review page to the Define Wiremock page" in {
        //          navigator.nextPage(ProduceApiReviewNameDescriptionPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiHowToAddWiremockController.onPageLoad(NormalMode)
        //        }
        // TODO: remove when bringing back with Wiremock
        "must go from the API review page to the Egress Availability page" in {
          navigator.nextPage(ProduceApiReviewNameDescriptionPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiEgressAvailabilityController.onPageLoad()
        }
        "must go from the Add Prefixes page to the Enter Prefixes page if user answered Yes" in {
          val userAnswers = emptyUserAnswers.set(ProduceApiAddPrefixesPage, true).get
          navigator.nextPage(ProduceApiAddPrefixesPage, NormalMode, userAnswers) mustBe controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(NormalMode)
        }
        "must go from the Add Prefixes page to the HoDs page if user answered No" in {
          val userAnswers = emptyUserAnswers.set(ProduceApiAddPrefixesPage, false).get
          navigator.nextPage(ProduceApiAddPrefixesPage, NormalMode, userAnswers) mustBe controllers.myapis.produce.routes.ProduceApiHodController.onPageLoad(NormalMode)
        }
        "must go from the Define Wiremock page to the WireMock editor page" in {
          navigator.nextPage(ProduceApiHowToAddWiremockPage, NormalMode, emptyUserAnswers.set[ProduceApiHowToAddWiremock](ProduceApiHowToAddWiremockPage, ProduceApiHowToAddWiremock.Editor).get) mustBe controllers.myapis.produce.routes.ProduceApiEnterWiremockController.onPageLoad(NormalMode)
        }
        "must go from the Define Wiremock page to the WireMock upload page" in {
          navigator.nextPage(ProduceApiHowToAddWiremockPage, NormalMode, emptyUserAnswers.set[ProduceApiHowToAddWiremock](ProduceApiHowToAddWiremockPage, ProduceApiHowToAddWiremock.Upload).get) mustBe controllers.myapis.produce.routes.ProduceApiUploadWiremockController.onPageLoad(NormalMode)
        }
        "must go from the Wiremock Editor page to the Add Prefixes page" in {
          navigator.nextPage(ProduceApiEnterWiremockPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiAddPrefixesController.onPageLoad(NormalMode)
        }
        "must go from the API Egress Prefixes page to the HoD page" in {
          navigator.nextPage(ProduceApiEgressPrefixesPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiHodController.onPageLoad(NormalMode)
        }
        "must go from the API HOD page to the API domain page" in {
          navigator.nextPage(ProduceApiHodPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiDomainController.onPageLoad(NormalMode)
        }
        "must go from the API domain page to the API status page" in {
          navigator.nextPage(ProduceApiDomainPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiStatusController.onPageLoad(NormalMode)
        }
        "must go from the API status page to the API passthrough page for support user" in {
          val userAnswers = emptyUserAnswers.set(ProduceApiStartPage, FakeUser.copy(permissions = Permissions(false, true, false))).get
          navigator.nextPage(ProduceApiStatusPage, NormalMode, userAnswers) mustBe controllers.myapis.produce.routes.ProduceApiPassthroughController.onPageLoad(NormalMode)
        }
        "must go from the API status page to the check your answers page for non-support user" in {
          val userAnswers = emptyUserAnswers.set(ProduceApiStartPage, FakeUser.copy(permissions = Permissions(false, false, false))).get
          navigator.nextPage(ProduceApiStatusPage, NormalMode, userAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the API passthrough page to the API check your answers page" in {
          navigator.nextPage(ProduceApiPassthroughPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
      }

      "during the Update an API journey" - {
        "must start with the Before you Start page" in {
          navigator.nextPage(UpdateApiStartPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiHowToUpdateController.onPageLoad(NormalMode)
        }
        "must go from the Enter the OAS page to the Short description page" in {
          navigator.nextPage(UpdateApiEnterOasPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiShortDescriptionController.onPageLoad(NormalMode)
        }
        "must go from the Review a short description page to the API review page" in {
          navigator.nextPage(UpdateApiShortDescriptionPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiReviewNameDescriptionController.onPageLoad(NormalMode)
        }
        // TODO: bring back with Wiremock
        //        "must go from the API review page to the Define Wiremock page" in {
        //          navigator.nextPage(UpdateApiReviewNameDescriptionPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiHowToAddWiremockController.onPageLoad(NormalMode)
        //        }
        // TODO: remove when bringing back with Wiremock
        "must go from the API review page to the Add Prefixes page" in {
          navigator.nextPage(UpdateApiReviewNameDescriptionPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiAddPrefixesController.onPageLoad(NormalMode)
        }
        "must go from the Add Prefixes page to the HoDs page if user answered No" in {
          val userAnswers = emptyUserAnswers.set(UpdateApiAddPrefixesPage, false).get
          navigator.nextPage(UpdateApiAddPrefixesPage, NormalMode, userAnswers) mustBe controllers.myapis.update.routes.UpdateApiHodController.onPageLoad(NormalMode)
        }
        "must go from the Define Wiremock page to the WireMock editor page" in {
          navigator.nextPage(UpdateApiHowToAddWiremockPage, NormalMode, emptyUserAnswers.set[ProduceApiHowToAddWiremock](UpdateApiHowToAddWiremockPage, ProduceApiHowToAddWiremock.Editor).get) mustBe controllers.myapis.update.routes.UpdateApiEnterWiremockController.onPageLoad(NormalMode)
        }
        "must go from the Define Wiremock page to the WireMock upload page" in {
          navigator.nextPage(UpdateApiHowToAddWiremockPage, NormalMode, emptyUserAnswers.set[ProduceApiHowToAddWiremock](UpdateApiHowToAddWiremockPage, ProduceApiHowToAddWiremock.Upload).get) mustBe controllers.myapis.update.routes.UpdateApiUploadWiremockController.onPageLoad(NormalMode)
        }
        "must go from the Wiremock Editor page to the Add Prefixes page" in {
          navigator.nextPage(UpdateApiEnterWiremockPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiAddPrefixesController.onPageLoad(NormalMode)
        }
        "must go from the Upload OAS page to the OAS editor page" in {
          navigator.nextPage(UpdateApiUploadOasPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiEnterOasController.onPageLoadWithUploadedOas(NormalMode)
        }
        "must go from the Review Hod to the Review domain and subdomain page" in {
          navigator.nextPage(UpdateApiHodPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiDomainController.onPageLoad(NormalMode)
        }
        "must go from the Domains page to the API status page" in {
          navigator.nextPage(UpdateApiDomainPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiReviewApiStatusController.onPageLoad(NormalMode)
        }
        "must go from the Review the API Status page to the Check your answers page" in {
          navigator.nextPage(UpdateApiStatusPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the Upload Wiremock page to the wiremock editor page" in {
          navigator.nextPage(UpdateApiUploadWiremockPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiEnterWiremockController.onPageLoadWithUploadedWiremock(NormalMode)
        }
      }

      "during the Cancel access request journey" - {
        "must start with the Select APIs page" in {
          navigator.nextPage(CancelAccessRequestStartPage, NormalMode, emptyUserAnswers) mustBe controllers.application.cancelaccessrequest.routes.CancelAccessRequestSelectApiController.onPageLoad(NormalMode)
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

      "during the Produce an API journey" - {
        "must go from Owning Team page to the Check Your Answers page" in {
          navigator.nextPage(ProduceApiChooseTeamPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the OAS editor page to the Short Description page" in {
          navigator.nextPage(ProduceApiEnterOasPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiShortDescriptionController.onPageLoad(CheckMode)
        }
        "must go from the Short Description page to the Preview Name/Description page" in {
          navigator.nextPage(ProduceApiShortDescriptionPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onPageLoad(CheckMode)
        }
        "must go from the Preview Name/Description page to the Check Your Answers page" in {
          navigator.nextPage(ProduceApiReviewNameDescriptionPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the Wiremock Editor page to the Check Your Answers page" in {
          navigator.nextPage(ProduceApiEnterWiremockPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the Add Prefixes page to the Enter Prefixes page if user answered Yes" in {
          val userAnswers = emptyUserAnswers.set(ProduceApiAddPrefixesPage, true).get
          navigator.nextPage(ProduceApiAddPrefixesPage, CheckMode, userAnswers) mustBe controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(CheckMode)
        }
        "must go from the Add Prefixes page to the Check Your Answers page if user answered No" in {
          val userAnswers = emptyUserAnswers.set(ProduceApiAddPrefixesPage, false).get
          navigator.nextPage(ProduceApiAddPrefixesPage, CheckMode, userAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the Egress Prefixes page to the Check Your Answers page" in {
          navigator.nextPage(ProduceApiEgressPrefixesPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the HoD page to the Egress Prefixes page" in {
          navigator.nextPage(ProduceApiHodPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the Domain/Subdomain page to the Egress Prefixes page" in {
          navigator.nextPage(ProduceApiDomainPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the API status page to the Egress Prefixes page" in {
          navigator.nextPage(ProduceApiStatusPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the API passthrough page to the Egress Prefixes page" in {
          navigator.nextPage(ProduceApiPassthroughPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
        }
        "must go from the Upload wiremock page to the wiremock editor page" in {
          navigator.nextPage(ProduceApiUploadWiremockPage, NormalMode, emptyUserAnswers) mustBe controllers.myapis.produce.routes.ProduceApiEnterWiremockController.onPageLoadWithUploadedWiremock(NormalMode)
        }
      }

      "during the Update an API journey" - {
        "must go from the Upload OAS page to the OAS editor page" in {
          navigator.nextPage(UpdateApiUploadOasPage, CheckMode, emptyUserAnswers) mustBe controllers.myapis.update.routes.UpdateApiEnterOasController.onPageLoadWithUploadedOas(CheckMode)
        }
        "must go from the Add Prefixes page to the Check Your Answers page if user answered No" in {
          val userAnswers = emptyUserAnswers.set(UpdateApiAddPrefixesPage, false).get
          navigator.nextPage(UpdateApiAddPrefixesPage, CheckMode, userAnswers) mustBe controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onPageLoad()
        }

      }
      
    }
  }

  private def buildUserAnswers(context: AddAnApiContext): UserAnswers = {
    UserAnswers("id").set(AddAnApiContextPage, context).toOption.value
  }

}
