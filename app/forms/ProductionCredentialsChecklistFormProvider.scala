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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

class ProductionCredentialsChecklistFormProvider @Inject() extends Mappings {

  import ProductionCredentialsChecklistFormProvider._

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("productionCredentialsChecklist.error.required")
        .verifying(booleanTrueConstraint("productionCredentialsChecklist.error.invalid"))
    )

}

object ProductionCredentialsChecklistFormProvider {

  def booleanTrueConstraint(invalidMessage: String): Constraint[Boolean] = Constraint("constraints.booleanTrue") {
    boolean =>
      if (boolean) {
        Valid
      }
      else {
        Invalid(Seq(ValidationError(invalidMessage)))
      }
  }

}
