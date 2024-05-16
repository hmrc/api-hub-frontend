/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import fakes.FakeDomains
import fakes.FakeDomains.data._
import generators.ApiDetailGenerators
import models.api.ApiDetailLenses._
import models.api.{Domain, SubDomain}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}

class DomainsSpec extends AnyFreeSpec with Matchers with OptionValues with ApiDetailGenerators with TableDrivenPropertyChecks {

  import DomainsSpec._

  "getDomain" - {
    "must return the correct domain given a code when it exists" in {
      FakeDomains.getDomain(domain1.code).value mustBe domain1
    }

    "must return None when no domain exists for the given code" in {
      FakeDomains.getDomain(unknownDomainCode) mustBe None
    }

    "must normalise codes in comparisons" in {
      val domainCode = s" ${uglyDomainCode.toUpperCase()} "
      uglyDomains.getDomain(domainCode).value mustBe uglyDomain
    }
  }

  "getDomainDescription(String)" - {
    "must return the correct domain given a code when it exists" in {
      FakeDomains.getDomainDescription(domain1.code) mustBe domain1.description
    }

    "must return the code when no domain exists for the given code" in {
      FakeDomains.getDomainDescription(unknownDomainCode) mustBe unknownDomainCode
    }
  }

  "getDomainDescription(Option[String])" - {
    "must return the description of a domain when it exists for the given code" in {
      FakeDomains.getDomainDescription(Some(domain1.code)).value mustBe domain1.description
    }

    "must return the code when no domain exists for the given code" in {
      FakeDomains.getDomainDescription(Some(unknownDomainCode)).value mustBe unknownDomainCode
    }

    "must return None when the input is None" - {
      FakeDomains.getDomainDescription(None) mustBe None
    }
  }

  "getDomainDescription(ApiDetail)" - {
    "must return the description of a domain when it exists for the API's domain" in {
      val apiDetail = sampleApiDetail().setDomain(domain2)

      FakeDomains.getDomainDescription(apiDetail).value mustBe domain2.description
    }

    "must return the code when no domain exists for the API's domain" in {
      val apiDetail = sampleApiDetail().setDomain(unknownDomainCode)
      FakeDomains.getDomainDescription(apiDetail).value mustBe unknownDomainCode
    }

    "must return None when the API does not have a domain" - {
      val apiDetail = sampleApiDetail().setDomain(None)
      FakeDomains.getDomainDescription(apiDetail) mustBe None
    }
  }

  "getSubDomain" - {
    "must return the correct sub-domain for the domain and sub-domain codes" in {
      val codes = Table(
        ("domain code", "sub-domain code", "expected"),
        (domain1.code, subDomain1_2.code, Some(subDomain1_2)),
        (domain1.code, subDomain2_2.code, None),
        (unknownDomainCode, subDomain1_2.code, None),
        (domain1.code, unknownSubDomainCode, None)
      )

      forAll(codes) {(domainCode: String, subDomainCode: String, expected: Option[SubDomain]) =>
        FakeDomains.getSubDomain(domainCode, subDomainCode) mustBe expected
      }
    }

    "must normalise codes in comparisons" in {
      val domainCode = s" ${uglyDomainCode.toUpperCase()} "
      val subDomainCode = s" ${uglySubDomainCode.toUpperCase()} "

      uglyDomains.getSubDomain(domainCode, subDomainCode).value mustBe uglySubDomain
    }
  }

  "getSubDomainDescription(String, String)" - {
    "must return the correct description for the domain and sub-domain codes" in {
      val codes = Table(
        ("domain code", "sub-domain code", "expected"),
        (domain1.code, subDomain1_3.code, subDomain1_3.description),
        (domain1.code, subDomain2_1.code, subDomain2_1.code),
        (unknownDomainCode, subDomain1_3.code, subDomain1_3.code),
        (domain1.code, unknownSubDomainCode, unknownSubDomainCode),
        (unknownDomainCode, unknownSubDomainCode, unknownSubDomainCode)
      )

      forAll(codes) {(domainCode: String, subDomainCode: String, expected: String) =>
        FakeDomains.getSubDomainDescription(domainCode, subDomainCode) mustBe expected
      }
    }
  }

  "getSubDomainDescription(Option[String], Option[String])" - {
    "must return the correct description for the domain and sub-domain codes" in {
      forAll(allOptionalDomainSubDomainScenarios) {(domainCode: Option[String], subDomainCode: Option[String], expected: Option[String]) =>
        FakeDomains.getSubDomainDescription(domainCode, subDomainCode) mustBe expected
      }
    }
  }

  "getSubDomainDescription(ApiDetail)" - {
    "must return the correct description for an API" in {
      forAll(allOptionalDomainSubDomainScenarios) {(domainCode: Option[String], subDomainCode: Option[String], expected: Option[String]) =>
        val apiDetail = sampleApiDetail()
          .setDomain(domainCode)
          .setSubDomain(subDomainCode)

        FakeDomains.getSubDomainDescription(apiDetail) mustBe expected
      }
    }
  }

}

object DomainsSpec extends TableDrivenPropertyChecks {

  val uglyDomainCode = "  TeSt-UgLy-CoDe "
  val uglySubDomainCode = ""

  val uglySubDomain: SubDomain = SubDomain(
    code = uglySubDomainCode,
    description =  s"Description of $uglySubDomainCode"
  )

  val uglyDomain: Domain = Domain(
    code = uglyDomainCode,
    description =  s"Description of $uglyDomainCode",
    subDomains = Seq(uglySubDomain)
  )

  val uglyDomains: Domains = new Domains {
    override def domains: Seq[Domain] = Seq(uglyDomain)
  }

  val allOptionalDomainSubDomainScenarios: TableFor3[Option[String], Option[String], Option[String]] = Table(
    ("domain code", "sub-domain code", "expected"),
    (Some(domain1.code), Some(subDomain1_3.code), Some(subDomain1_3.description)),
    (Some(domain1.code), Some(subDomain2_1.code), Some(subDomain2_1.code)),
    (Some(unknownDomainCode), Some(subDomain1_3.code), Some(subDomain1_3.code)),
    (Some(domain1.code), Some(unknownSubDomainCode), Some(unknownSubDomainCode)),
    (Some(unknownDomainCode), Some(unknownSubDomainCode), Some(unknownSubDomainCode)),
    (Some(domain2.code), None, None),
    (None, Some(subDomain2_1.code), Some(subDomain2_1.code)),
    (None, None, None)
  )

}
