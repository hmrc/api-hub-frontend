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

import controllers.routes
import models.*
import models.myapis.produce.ProduceApiChooseEgress
import models.myapis.produce.ProduceApiHowToCreate.{Editor, Upload}
import pages.*
import pages.application.accessrequest.{ProvideSupportingInformationPage, RequestProductionAccessPage, RequestProductionAccessSelectApisPage, RequestProductionAccessStartPage}
import pages.application.cancelaccessrequest.{CancelAccessRequestApplicationPage, CancelAccessRequestConfirmPage, CancelAccessRequestSelectApiPage, CancelAccessRequestStartPage}
import pages.application.register.{RegisterApplicationNamePage, RegisterApplicationStartPage, RegisterApplicationTeamPage}
import pages.myapis.produce.*
import pages.myapis.update.UpdateApiStartPage
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case AddAnApiApiPage => addAnApiApiIdNextPage
    case AddAnApiSelectApplicationPage => addAnApiSelectApplicationNextPage(NormalMode)
    case AddAnApiSelectEndpointsPage => addAnApiSelectEndpointsNextPage(NormalMode)
    case ApiPolicyConditionsDeclarationPage => apiPolicyConditionsDeclarationNextPage(NormalMode)
    case CreateTeamStartPage => _ => controllers.team.routes.CreateTeamNameController.onPageLoad(NormalMode)
    case CreateTeamNamePage => _ => controllers.team.routes.ManageTeamMembersController.onPageLoad()
    case CreateTeamMembersPage => _ => controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad()
    case CreateTeamMemberPage => _ => controllers.team.routes.ManageTeamMembersController.onPageLoad()
    case RegisterApplicationStartPage => _ => controllers.application.register.routes.RegisterApplicationNameController.onPageLoad(NormalMode)
    case RegisterApplicationNamePage => _ => controllers.application.register.routes.RegisterApplicationTeamController.onPageLoad(NormalMode)
    case RegisterApplicationTeamPage => _ => controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad()
    case RequestProductionAccessStartPage => _ => controllers.application.accessrequest.routes.RequestProductionAccessSelectApisController.onPageLoad(NormalMode)
    case RequestProductionAccessSelectApisPage => _ => controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(NormalMode)
    case ProvideSupportingInformationPage => _ => controllers.application.accessrequest.routes.RequestProductionAccessController.onPageLoad()
    case RequestProductionAccessPage => _ => controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest()
    case ProduceApiStartPage => _ => controllers.myapis.produce.routes.ProduceApiBeforeYouStartController.onPageLoad()
    case ProduceApiBeforeYouStartPage => _ => controllers.myapis.produce.routes.ProduceApiChooseTeamController.onPageLoad(NormalMode)
    case ProduceApiChooseTeamPage => _ => controllers.myapis.produce.routes.ProduceApiHowToCreateController.onPageLoad(NormalMode)
    case ProduceApiUploadOasPage => _ => controllers.myapis.produce.routes.ProduceApiEnterOasController.onPageLoadWithUploadedOas(NormalMode)
    case ProduceApiEnterOasPage => _ => controllers.myapis.produce.routes.ProduceApiShortDescriptionController.onPageLoad(NormalMode)
    case ProduceApiHowToCreatePage => produceApiHowToCreateNextPage(NormalMode)
    case ProduceApiShortDescriptionPage => _ => controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onPageLoad(NormalMode)
    case ProduceApiReviewNameDescriptionPage => _ => controllers.myapis.produce.routes.ProduceApiEgressController.onPageLoad(NormalMode)
    case ProduceApiChooseEgressPage => produceApiChooseEgressNextPage(NormalMode)
    case ProduceApiEgressPrefixesPage => _ => controllers.myapis.produce.routes.ProduceApiHodController.onPageLoad(NormalMode)
    case ProduceApiHodPage => _ => controllers.myapis.produce.routes.ProduceApiDomainController.onPageLoad(NormalMode)
    case ProduceApiDomainPage => _ => controllers.myapis.produce.routes.ProduceApiStatusController.onPageLoad(NormalMode)
    case ProduceApiStatusPage => produceApiStatusNextPage(NormalMode)
    case ProduceApiPassthroughPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case CancelAccessRequestStartPage => _ => controllers.application.cancelaccessrequest.routes.CancelAccessRequestSelectApiController.onPageLoad(NormalMode)
    case CancelAccessRequestSelectApiPage => _ => controllers.application.cancelaccessrequest.routes.CancelAccessRequestConfirmController.onPageLoad(NormalMode)
    case CancelAccessRequestConfirmPage => cancelAccessRequestConfirmNextPage(NormalMode)
    case UpdateApiStartPage => _ => controllers.myapis.update.routes.UpdateApiHowToUpdateController.onPageLoad(NormalMode)
    case _ => _ => routes.IndexController.onPageLoad
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case AddAnApiSelectApplicationPage => addAnApiSelectApplicationNextPage(CheckMode)
    case AddAnApiSelectEndpointsPage => addAnApiSelectEndpointsNextPage(CheckMode)
    case ApiPolicyConditionsDeclarationPage => apiPolicyConditionsDeclarationNextPage(CheckMode)
    case CreateTeamNamePage => _ => controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad()
    case RegisterApplicationNamePage => _ => controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad()
    case RegisterApplicationTeamPage => _ => controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad()
    case RequestProductionAccessSelectApisPage => _ => controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(CheckMode)
    case ProvideSupportingInformationPage => _ => controllers.application.accessrequest.routes.RequestProductionAccessController.onPageLoad()
    case ProduceApiChooseTeamPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case ProduceApiEnterOasPage => _ => controllers.myapis.produce.routes.ProduceApiShortDescriptionController.onPageLoad(CheckMode)
    case ProduceApiShortDescriptionPage => _ => controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onPageLoad(CheckMode)
    case ProduceApiReviewNameDescriptionPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case ProduceApiChooseEgressPage => produceApiChooseEgressNextPage(CheckMode)
    case ProduceApiEgressPrefixesPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case ProduceApiHodPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case ProduceApiDomainPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case ProduceApiStatusPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case ProduceApiPassthroughPage => _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    case _ => _ => routes.IndexController.onPageLoad
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }

  private def addAnApiApiIdNextPage(userAnswers: UserAnswers): Call = {
    userAnswers.get(AddAnApiContextPage) match {
      case Some(AddAnApi) => routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode)
      case Some(AddEndpoints) => routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode, AddEndpoints)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def addAnApiSelectApplicationNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(AddAnApiContextPage)) match {
      case (NormalMode, Some(AddAnApi)) => routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode, AddAnApi)
      case (CheckMode, Some(AddAnApi)) => routes.AddAnApiCheckYourAnswersController.onPageLoad(AddAnApi)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def addAnApiSelectEndpointsNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(AddAnApiContextPage)) match {
      case (NormalMode, Some(context)) => routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode, context)
      case (CheckMode, Some(context)) => routes.AddAnApiCheckYourAnswersController.onPageLoad(context)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def apiPolicyConditionsDeclarationNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(AddAnApiContextPage)) match {
      case (_, Some(context)) => routes.AddAnApiCheckYourAnswersController.onPageLoad(context)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def cancelAccessRequestConfirmNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(CancelAccessRequestConfirmPage), userAnswers.get(CancelAccessRequestApplicationPage)) match {
      case (_, Some(true), _) => controllers.application.cancelaccessrequest.routes.CancelAccessRequestEndJourneyController.submitRequest()
      case (_, Some(false), Some(application)) => controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def produceApiHowToCreateNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(ProduceApiHowToCreatePage)) match {
      case (_, Some(Editor)) => controllers.myapis.produce.routes.ProduceApiEnterOasController.onPageLoad(mode)
      case (_, Some(Upload)) => controllers.myapis.produce.routes.ProduceApiUploadOasController.onPageLoad(mode)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def produceApiChooseEgressNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    (mode, userAnswers.get(ProduceApiChooseEgressPage)) match {
      case (_, Some(ProduceApiChooseEgress(_, true))) => controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(mode)
      case (NormalMode, Some(ProduceApiChooseEgress(_, false))) => controllers.myapis.produce.routes.ProduceApiHodController.onPageLoad(mode)
      case (CheckMode, Some(ProduceApiChooseEgress(_, false))) => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }
  
  private def produceApiStatusNextPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    userAnswers.get(ProduceApiStartPage) match {
      case Some(user) if user.permissions.canSupport => controllers.myapis.produce.routes.ProduceApiPassthroughController.onPageLoad(mode)
      case _ => controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
    }
  }

}
