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

import models.ApiPolicyConditionsDeclaration
import models.team.Team
import models.myapis.produce.*
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages.*
import pages.application.accessrequest.RequestProductionAccessSelectApisPage
import pages.application.cancelaccessrequest.CancelAccessRequestConfirmPage
import pages.application.register.RegisterApplicationTeamPage
import pages.myapis.produce.*
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators with TeamGenerator {

  implicit lazy val arbitraryProduceApiShortDescriptionUserAnswersEntry: Arbitrary[(ProduceApiShortDescriptionPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ProduceApiShortDescriptionPage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryProduceApiReviewNameDescriptionUserAnswersEntry: Arbitrary[(ProduceApiReviewNameDescriptionPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ProduceApiReviewNameDescriptionPage.type]
        value <- arbitrary[ProduceApiReviewNameDescription].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryProduceApiChooseTeamUserAnswersEntry: Arbitrary[(ProduceApiChooseTeamPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ProduceApiChooseTeamPage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCancelAccessRequestConfirmUserAnswersEntry: Arbitrary[(CancelAccessRequestConfirmPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CancelAccessRequestConfirmPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryRequestProductionAccessSelectApisUserAnswersEntry: Arbitrary[(RequestProductionAccessSelectApisPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[RequestProductionAccessSelectApisPage.type]
        value <- arbitrary[String].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryProduceApiEnterOasUserAnswersEntry: Arbitrary[(ProduceApiEnterOasPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ProduceApiEnterOasPage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryProduceApiHowToCreateUserAnswersEntry: Arbitrary[(ProduceApiHowToCreatePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ProduceApiHowToCreatePage.type]
        value <- arbitrary[ProduceApiHowToCreate].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryRegisterApplicationTeamUserAnswersEntry: Arbitrary[(RegisterApplicationTeamPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[RegisterApplicationTeamPage.type]
        value <- arbitrary[Team].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCreateTeamMemberUserAnswersEntry: Arbitrary[(CreateTeamMemberPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CreateTeamMemberPage.type]
        value <- genLegalUnicodeString.map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCreateTeamNameUserAnswersEntry: Arbitrary[(CreateTeamNamePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CreateTeamNamePage.type]
        value <- genLegalUnicodeString.map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryApiPolicyConditionsDeclarationPageUserAnswersEntry: Arbitrary[(ApiPolicyConditionsDeclarationPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ApiPolicyConditionsDeclarationPage.type]
        value <- arbitrary[ApiPolicyConditionsDeclaration].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAddAnApiSelectEndpointsUserAnswersEntry: Arbitrary[(AddAnApiSelectEndpointsPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[AddAnApiSelectEndpointsPage.type]
        value <- arbitrary[Set[Set[String]]].map(Json.toJson(_))
      } yield (page, value)
    }

}
