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

package models.myapis.produce

import controllers.actions.FakeApplication
import models.api.ApiDetailLensesSpec.sampleOas
import models.api.*
import models.application.ApplicationLenses.*
import models.application.{Api, SelectedEndpoint}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.time.Instant

class ProduceApiEgressPrefixesSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  "ProduceApiEgressPrefixes" - {
    "getMappings must return correct values" in {
      val model = ProduceApiEgressPrefixes(Seq.empty, Seq("existing1->replacement1", "existing2->replacement2"))
      val expected = Seq(ProduceApiEgressPrefixMapping("existing1", "replacement1"), ProduceApiEgressPrefixMapping("existing2", "replacement2"))

      model.getMappings mustBe expected
    }

    "unapply must decompose model correctly" in {
      val prefixes = Seq("prefix1", "prefix2")
      val mappings = Seq("existing1->replacement1", "existing2->replacement2")
      val model = ProduceApiEgressPrefixes(prefixes, mappings)
      val expected = Some((prefixes, mappings))

      ProduceApiEgressPrefixes.unapply(model) mustBe expected
    }
    
    "isEmpty must return correct value" in {
      forAll(Table(
        ("Prefixes", "Mappings", "Expected"),
        (Seq.empty, Seq.empty, true),
        (Seq("/prefix"), Seq.empty, false),
        (Seq.empty, Seq("/aa->/bb"), false),
        (Seq("/prefix"), Seq("/aa->/bb"), false),
      )) { (prefixes, mappings, expectedResult) =>
        ProduceApiEgressPrefixes(prefixes, mappings).isEmpty mustBe expectedResult
      }
    }
  }
}
