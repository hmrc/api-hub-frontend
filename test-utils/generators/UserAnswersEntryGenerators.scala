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
import models.application.TeamMember
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages._
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

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

  implicit lazy val arbitraryConfirmAddTeamMemberUserAnswersEntry: Arbitrary[(ConfirmAddTeamMemberPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ConfirmAddTeamMemberPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryTeamMembersUserAnswersEntry: Arbitrary[(TeamMembersPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[TeamMembersPage.type]
        value <- arbitrary[Seq[TeamMember]].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryQuestionAddTeamMembersUserAnswersEntry: Arbitrary[(QuestionAddTeamMembersPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[QuestionAddTeamMembersPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryApplicationNameUserAnswersEntry: Arbitrary[(ApplicationNamePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ApplicationNamePage.type]
        value <- genLegalUnicodeString.map(Json.toJson(_))
      } yield (page, value)
    }

}
