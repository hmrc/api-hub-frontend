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

package forms.admin

import forms.behaviours.OptionFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import viewmodels.admin.Decision

class ApprovalDecisionFormProviderSpec extends OptionFieldBehaviours {

  private val form = new ApprovalDecisionFormProvider()()

  ".decision" - {
    val fieldName = "decision"
    val requiredKey = "accessRequest.decision.required"
    val invalidKey = "accessRequest.decision.invalid"

    behave like optionsField(
      form,
      fieldName,
      Decision.values,
      FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".rejectedReason" - {
    val fieldName = "rejectedReason"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.alphaStr
    )
  }

}
