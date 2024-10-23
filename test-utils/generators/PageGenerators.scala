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

package generators

import org.scalacheck.Arbitrary
import pages.*
import pages.application.accessrequest.RequestProductionAccessSelectApisPage
import pages.application.cancelaccessrequest.{CancelAccessRequestConfirmPage, CancelAccessRequestSelectApiPage}
import pages.application.register.RegisterApplicationTeamPage
import pages.myapis.produce.*

trait PageGenerators {

  implicit lazy val arbitraryProduceApiPassthroughPage: Arbitrary[ProduceApiPassthroughPage.type] =
    Arbitrary(ProduceApiPassthroughPage)

  implicit lazy val arbitraryProduceApiHodPage: Arbitrary[ProduceApiHodPage.type] =
    Arbitrary(ProduceApiHodPage)
  
  implicit lazy val arbitraryProduceApiDomainPage: Arbitrary[ProduceApiDomainPage.type] =
    Arbitrary(ProduceApiDomainPage)

  implicit lazy val arbitraryProduceApiStatusPage: Arbitrary[ProduceApiStatusPage.type] =
    Arbitrary(ProduceApiStatusPage)

  implicit lazy val arbitraryProduceApiShortDescriptionPage: Arbitrary[ProduceApiShortDescriptionPage.type] =
    Arbitrary(ProduceApiShortDescriptionPage)

  implicit lazy val arbitraryProduceApiReviewNameDescriptionPage: Arbitrary[ProduceApiReviewNameDescriptionPage.type] =
    Arbitrary(ProduceApiReviewNameDescriptionPage)

  implicit lazy val arbitraryProduceApiChooseTeamPage: Arbitrary[ProduceApiChooseTeamPage.type] =
    Arbitrary(ProduceApiChooseTeamPage)

  implicit lazy val arbitraryCancelAccessRequestConfirmPage: Arbitrary[CancelAccessRequestConfirmPage.type] =
    Arbitrary(CancelAccessRequestConfirmPage)

  implicit lazy val arbitraryCancelAccessRequestSelectApiPage: Arbitrary[CancelAccessRequestSelectApiPage.type] =
    Arbitrary(CancelAccessRequestSelectApiPage)

  implicit lazy val arbitraryRequestProductionAccessSelectApisPage: Arbitrary[RequestProductionAccessSelectApisPage.type] =
    Arbitrary(RequestProductionAccessSelectApisPage)

  implicit lazy val arbitraryProduceApiEnterOasPage: Arbitrary[ProduceApiEnterOasPage.type] =
    Arbitrary(ProduceApiEnterOasPage)

  implicit lazy val arbitraryProduceApiHowToCreatePage: Arbitrary[ProduceApiHowToCreatePage.type] =
    Arbitrary(ProduceApiHowToCreatePage)

  implicit lazy val arbitraryRegisterApplicationTeamPage: Arbitrary[RegisterApplicationTeamPage.type] =
    Arbitrary(RegisterApplicationTeamPage)

  implicit lazy val arbitraryCreateTeamMemberPage: Arbitrary[CreateTeamMemberPage.type] =
    Arbitrary(CreateTeamMemberPage)

  implicit lazy val arbitraryCreateTeamNamePage: Arbitrary[CreateTeamNamePage.type] =
    Arbitrary(CreateTeamNamePage)

  implicit lazy val arbitraryApiPolicyConditionsDeclarationPage: Arbitrary[ApiPolicyConditionsDeclarationPage.type] =
    Arbitrary(ApiPolicyConditionsDeclarationPage)

  implicit lazy val arbitraryAddAnApiSelectEndpointsPage: Arbitrary[AddAnApiSelectEndpointsPage.type] =
    Arbitrary(AddAnApiSelectEndpointsPage)

}
