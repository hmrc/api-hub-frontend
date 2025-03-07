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

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject
import scala.util.matching.Regex

class ForcePublishPublisherReferenceFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("forcePublish.publisherReferenceInput.error.required").verifying(publisherReferenceConstraint)
    )

  private val publisherReferenceRegex: Regex = """^[a-z0-9\-]+$""".r

  private val publisherReferenceConstraint: Constraint[String] = Constraint[String] {
    value =>
      if (publisherReferenceRegex.matches(value)) {
        Valid
      }
      else {
        Invalid("forcePublish.publisherReferenceInput.error.invalid")
      }
  }

}
