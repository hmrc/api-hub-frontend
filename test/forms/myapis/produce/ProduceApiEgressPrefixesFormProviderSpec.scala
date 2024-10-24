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

package forms.myapis.produce

import forms.behaviours.StringFieldBehaviours
import models.myapis.produce.ProduceApiEgressPrefixes
import play.api.data.FormError

class ProduceApiEgressPrefixesFormProviderSpec extends StringFieldBehaviours {

  val form = new ProduceApiEgressPrefixesFormProvider()()

  "prefixes" - {
    val fieldName = "prefixes[]"
    val requiredKey = "produceApiEgressPrefix.prefixes.error.missing"
    val startWithSlashKey = "produceApiEgressPrefix.prefixes.error.startWithSlash"

    "must bind value if all prefix values are valid" in {
      val prefixes = Seq("/valid1", "/valid2", "/valid3")
      val result = form.bindFromRequest(Map(fieldName -> prefixes))
      result.errors must be(empty)
      result.value.value mustBe ProduceApiEgressPrefixes(prefixes, Seq())
    }

    "must fail if any prefix values are empty" in {
      val prefixes = Seq("/valid1", "", "/valid3")
      val result = form.bindFromRequest(Map(fieldName -> prefixes))
      result.errors must contain(FormError("prefixes[1]", requiredKey))
    }

    "must fail if any prefix values don't start with a / character" in {
      val prefixes = Seq("/valid1", "invalid", "/valid3")
      val result = form.bindFromRequest(Map(fieldName -> prefixes))
      result.errors must contain(FormError("prefixes", startWithSlashKey))
    }
  }

  "mappings" - {
    val fieldName = "mappings[]"
    val requiredKey = "produceApiEgressPrefix.mappings.error.missing"
    val startWithSlashKey = "produceApiEgressPrefix.mappings.error.startWithSlash"
    val separatorKey = "produceApiEgressPrefix.mappings.error.separator"

    "must bind value if all mapping values are valid" in {
      val mappings = Seq("/a->/b", "/aa->/bb", "/a/b->/b/c")
      val result = form.bindFromRequest(Map(fieldName -> mappings))
      result.errors must be(empty)
      result.value.value mustBe ProduceApiEgressPrefixes(Seq(), mappings)
    }

    "must fail if any mapping values are empty" in {
      val mappings = Seq("/a->/b", "", "/a/b->/b/c")
      val result = form.bindFromRequest(Map(fieldName -> mappings))
      result.errors must contain(FormError("mappings[1]", requiredKey))
    }

    "must fail if any mapping values don't contain the expected separator" in {
      val mappings = Seq("/a->/b", "/aa/bb", "/a/b->/b/c")
      val result = form.bindFromRequest(Map(fieldName -> mappings))
      result.errors must contain(FormError("mappings", separatorKey))
    }

    "must fail if any mapping values contain more than 1 of the expected separator" in {
      val mappings = Seq("/a->/b", "/aa->/bb->/cc", "/a/b->/b/c")
      val result = form.bindFromRequest(Map(fieldName -> mappings))
      result.errors must contain(FormError("mappings", separatorKey))
    }

    "must fail if the first part of any mapping value doesnt start with a slash" in {
      val mappings = Seq("/a->/b", "/aa->bb", "/a/b->/b/c")
      val result = form.bindFromRequest(Map(fieldName -> mappings))
      result.errors must contain(FormError("mappings", startWithSlashKey))
    }

    "must fail if the second part of any mapping value doesnt start with a slash" in {
      val mappings = Seq("/a->/b", "aa->/bb", "/a/b->/b/c")
      val result = form.bindFromRequest(Map(fieldName -> mappings))
      result.errors must contain(FormError("mappings", startWithSlashKey))
    }
  }
}
