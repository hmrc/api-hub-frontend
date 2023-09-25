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

import models.{AddAnApiSelectEndpoints, ProductionCredentialsChecklist}
import models.application.TeamMember
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators {

  implicit lazy val arbitraryAddAnApiSelectEndpoints: Arbitrary[AddAnApiSelectEndpoints] =
    Arbitrary {
      Gen.oneOf(AddAnApiSelectEndpoints.values)
    }

  implicit lazy val arbitraryProductionCredentialsChecklist: Arbitrary[ProductionCredentialsChecklist] =
    Arbitrary {
      Gen.oneOf(ProductionCredentialsChecklist.values)
    }

  implicit lazy val arbitraryTeamMember: Arbitrary[TeamMember] =
    Arbitrary {
      for {
        email <- arbitraryHmrcEmail.arbitrary
      } yield TeamMember(email)
    }

  lazy val arbitraryHmrcEmail: Arbitrary[String] =
    Arbitrary {
      for {
        domain <- Gen.oneOf("digital.hmrc.gov.uk", "hmrc.gov.uk")
        firstName <- Gen.alphaLowerStr
        lastName <- Gen.alphaLowerStr
      } yield s"$firstName.$lastName@$domain"
    }

  lazy val arbitraryNonHmrcEmail: Arbitrary[String] =
    Arbitrary {
      for {
        topLevelDomain <- Gen.alphaLowerStr
        domain <- Gen.alphaLowerStr
        firstName <- Gen.alphaLowerStr
        lastName <- Gen.alphaLowerStr
      } yield s"$firstName.$lastName@$domain.$topLevelDomain"
    }

  lazy val genLegalUnicodeChar: Gen[Char] = Arbitrary.arbChar.arbitrary.suchThat(
    c =>
      Character.isDefined(c)
        && Character.UnicodeBlock.of(c) != Character.UnicodeBlock.PRIVATE_USE_AREA
        && !Character.isISOControl(c)
  )

  lazy val genLegalUnicodeString: Gen[String] = Gen.nonEmptyListOf(genLegalUnicodeChar).map(_.mkString)

}
