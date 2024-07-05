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

import controllers.IndexControllerSpec.convertToAnyMustWrapper
import fakes.FakeHods
import models.api.Hod
import org.scalatest.freespec.AnyFreeSpec
import play.api.Configuration

class HodsSpec extends AnyFreeSpec {

  "getDescription" - {
    "returns item which matches specified code" in {
      FakeHods.getDescription("EMS") mustBe "Enterprise Matching Service"
    }
    "returns item which matches specified code ignoring case" in {
      FakeHods.getDescription("aPiM") mustBe "API Management (HIP)"
    }
    "returns code when no item matches" in {
      FakeHods.getDescription("unknown") mustBe "unknown"
    }
  }

  "items are sorted alphabetically by description" in {
    val hods = new HodsImpl(Configuration("hods" -> Map(
      "C1" -> "D2",
      "C2" -> "D3",
      "C3" -> "D1"
    )))

    hods.hods mustBe Seq(
      Hod("C3", "D1"),
      Hod("C1", "D2"),
      Hod("C2", "D3")
    )
  }
}

