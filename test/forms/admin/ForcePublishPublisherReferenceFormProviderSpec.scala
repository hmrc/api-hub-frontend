/*
 * Copyright 2025 HM Revenue & Customs
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

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class ForcePublishPublisherReferenceFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "forcePublish.publisherReferenceInput.error.required"
  val invalidKey = "forcePublish.publisherReferenceInput.error.invalid"

  val form = new ForcePublishPublisherReferenceFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.nonEmptyStringOf(Gen.oneOf(Gen.alphaLowerChar, Gen.numChar, Gen.const('-')))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldThatRejectsInvalidData(
      form,
      fieldName,
      invalidKey,
      Gen.oneOf(Seq("UPPER-CASE", "invalid-char-*", "no spaces"))
    )

  }

}
