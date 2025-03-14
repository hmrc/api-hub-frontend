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

import models.application.TeamMember
import models.ApiPolicyConditionsDeclaration
import models.myapis.produce.*
import org.scalacheck.{Arbitrary, Gen}
import viewmodels.admin.Decision
import viewmodels.admin.Decision.{Approve, Reject}

trait ModelGenerators {

  implicit lazy val arbitraryProduceApiHowToAddWiremock: Arbitrary[ProduceApiHowToAddWiremock] =
    Arbitrary {
      Gen.oneOf(ProduceApiHowToAddWiremock.values.toSeq)
    }

  implicit lazy val arbitraryProduceApiEgressPrefixes: Arbitrary[ProduceApiEgressPrefixes] =
    Arbitrary {
      for {
        prefix <- Gen.asciiStr
        mappingExisting <- Gen.asciiStr 
        mappingReplacement <- Gen.asciiStr 
      } yield ProduceApiEgressPrefixes(Seq(s"/$prefix"), Seq(s"/${mappingExisting}->/${mappingReplacement}"))
    }

  implicit lazy val arbitraryProduceApiDomain: Arbitrary[ProduceApiDomainSubdomain] =
    Arbitrary {
      for {
        domain <- Gen.asciiStr
        subDomain <- Gen.asciiStr
      } yield ProduceApiDomainSubdomain(domain, subDomain)
    }

  implicit lazy val arbitraryProduceApiReviewNameDescription: Arbitrary[ProduceApiReviewNameDescription] =
    Arbitrary {
      Gen.oneOf(ProduceApiReviewNameDescription.values)
    }

  implicit lazy val arbitraryProduceApiHowToCreate: Arbitrary[ProduceApiHowToCreate] =
    Arbitrary {
      Gen.oneOf(ProduceApiHowToCreate.values.toSeq)
    }

  implicit lazy val arbitraryApiPolicyConditionsDeclaration: Arbitrary[ApiPolicyConditionsDeclaration] =
    Arbitrary {
      Gen.oneOf(ApiPolicyConditionsDeclaration.values)
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

  lazy val arbitraryApproval: Arbitrary[Decision] =
    Arbitrary {
      for {
        approval <- Gen.oneOf(Approve, Reject)
      } yield approval
    }

}
