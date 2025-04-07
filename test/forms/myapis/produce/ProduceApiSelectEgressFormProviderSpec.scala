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
import play.api.data.FormError
import viewmodels.myapis.produce.ProduceApiSelectEgressFormViewModel

class ProduceApiSelectEgressFormProviderSpec extends StringFieldBehaviours {

  private val form = new ProduceApiSelectEgressForm()()
  private val validEgress = "egress1"

  "validation" - {
    "must pass if egress is selected and 'continue' is clicked" in {
      val result = form.bindFromRequest(Map("value" -> Seq("egress1"), "noegress" -> Seq("false")))
      result.errors must be(empty)
      result.value.value mustBe ProduceApiSelectEgressFormViewModel(validEgress, false)
    }

    "must pass if egress is not selected and 'continue without an egress' is clicked" in {
      val result = form.bindFromRequest(Map("value" -> Seq(""), "noegress" -> Seq("true")))
      result.errors must be(empty)
      result.value.value mustBe ProduceApiSelectEgressFormViewModel("", true)
    }

    "must pass but egress value must be cleared if egress is selected and 'continue without an egress' is clicked" in {
      val result = form.bindFromRequest(Map("value" -> Seq(validEgress), "noegress" -> Seq("true")))
      result.errors must be(empty)
      result.value.value mustBe ProduceApiSelectEgressFormViewModel("", true)
    }

    "must fail with an error if egress is not selected and 'continue' is clicked" in {
      val result = form.bindFromRequest(Map("value" -> Seq(""), "noegress" -> Seq("false")))
      result.errors must contain(FormError("", "Select an egress, or use the 'Continue without an egress' button"))
    }
  }
}
