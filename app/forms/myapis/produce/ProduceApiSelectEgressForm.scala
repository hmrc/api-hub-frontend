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

import forms.mappings.Mappings
import play.api.data.Forms.{default, mapping}
import play.api.data.{Form, Forms}
import viewmodels.myapis.produce.ProduceApiSelectEgressFormViewModel

import javax.inject.Inject

class ProduceApiSelectEgressForm @Inject() extends Mappings {

  def apply(): Form[ProduceApiSelectEgressFormViewModel] = Form(
    mapping(
      "value" -> default(text(), ""),
      "noegress" -> boolean()
    )(ProduceApiSelectEgressFormViewModel.apply)(formValues => Some(Tuple.fromProductTyped(formValues))).transform(
      formValues => if (formValues.noEgress) formValues.copy(value = "") else formValues,
      identity
    ).verifying(
      "Select an egress, or use the 'Continue without an egress' button",
      fields => fields match {
        case ProduceApiSelectEgressFormViewModel(_, true) => true
        case ProduceApiSelectEgressFormViewModel(egress, false) => !egress.isBlank
      }
    )
  )
}
