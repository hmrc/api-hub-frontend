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

package forms

import controllers.actions.FakeApplication
import forms.behaviours.CheckboxFieldBehaviours
import generators.ApiDetailGenerators
import models.AvailableEndpoints
import play.api.data.FormError

class AddAnApiSelectEndpointsFormProviderSpec extends CheckboxFieldBehaviours with ApiDetailGenerators {

  private val apiDetail = sampleApiDetail()
  private val form = new AddAnApiSelectEndpointsFormProvider()(apiDetail, FakeApplication)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "addAnApiSelectEndpoints.error.required"

    behave like checkboxField[Set[String]](
      form,
      fieldName,
      validValues  = AvailableEndpoints(apiDetail, FakeApplication).keySet.toSeq,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }

}
