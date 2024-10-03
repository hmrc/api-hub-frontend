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

import fakes.FakeEmailDomains
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
 
class EmailDomainsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks{

"emailAddressHasValidDomain" in {
    forAll(Table(
      ("Email", "Has valid domain"),
      ("some@example.com", false),
      ("@some@example.com", false),
      ("nope@some@example.com", false),
      ("@example.com", false),
      ("example.com", false),
      ("example", false),
      ("user@hmrc.gov.uk", true),
      ("some.user@digital.hmrc.gov.uk", true),
      ("some.user@DIGITAL.hMrC.gOv.UK", true),
      ("  some.user@digital.hmrc.gov.uk   ", true),
      ("some@user@ibm.com", true),
      ("some@user@ibm.com.uk", false),
      ("some.user@nope.hmrc.gov.uk", false),
    )) { (email, isValid) =>
      FakeEmailDomains.emailAddressHasValidDomain(email) mustBe isValid
    }}
}

