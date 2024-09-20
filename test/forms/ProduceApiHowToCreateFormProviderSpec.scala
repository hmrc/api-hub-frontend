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

package forms

import forms.behaviours.OptionFieldBehaviours
import forms.myapis.produce.ProduceApiHowToCreateFormProvider
import models.myapis.produce.ProduceApiHowToCreate
import play.api.data.FormError

class ProduceApiHowToCreateFormProviderSpec extends OptionFieldBehaviours {

  val form = new ProduceApiHowToCreateFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "myApis.produce.howtocreate.error.required"

    behave like optionsField[ProduceApiHowToCreate](
      form,
      fieldName,
      validValues  = ProduceApiHowToCreate.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
