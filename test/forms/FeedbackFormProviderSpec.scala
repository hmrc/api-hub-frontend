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

import forms.behaviours.FieldBehaviours
import models.Feedback
import models.Feedback.FeedbackType.*
import play.api.data.FormError

class FeedbackFormProviderSpec extends FieldBehaviours {

  private val form = new FeedbackFormProvider()()

  private val feedback = Feedback(
    `type` = Other,
    otherType = Some("otherType"),
    rate = 5,
    comments = "comments",
    allowContact = true,
    email = Some("email@email.com"),
  )
  private val feedbackValues: Map[String, String] = Map(
    "type" -> feedback.`type`.toString,
    "otherType" -> feedback.otherType.getOrElse(""),
    "rate" -> feedback.rate.toString,
    "comments" -> feedback.comments,
    "allowContact" -> feedback.allowContact.toString,
    "allowContactEmail" -> feedback.email.getOrElse(""),
  )

  ".type" - {

    val fieldName = "type"
    val boundedForm = form.bind(
      feedbackValues - fieldName
    )
    val requiredKey = "feedback.type.error.required"

    behave like mandatoryField(
      boundedForm,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".otherType" - {

    val fieldName = "otherType"
    val boundedForm = form.bind(
      feedbackValues - fieldName
    )
    val requiredKey = "feedback.otherType.error.required"

    "fail when the type is 'other' and otherType is empty" in {
      boundedForm.errors.toSet mustEqual Set(FormError("", requiredKey))
    }
  }

  ".rate" - {

    val fieldName = "rate"
    val boundedForm = form.bind(
      feedbackValues - fieldName
    )
    val requiredKey = "feedback.rate.error.required"

    behave like mandatoryField(
      boundedForm,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".comments" - {

    val fieldName = "comments"
    val boundedForm = form.bind(
      feedbackValues - fieldName
    )
    val requiredKey = "feedback.comments.error.required"

    behave like mandatoryField(
      boundedForm,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".allowContact" - {

    val fieldName = "allowContact"
    val boundedForm = form.bind(
      feedbackValues - fieldName
    )
    val requiredKey = "feedback.allowContact.error.required"

    behave like mandatoryField(
      boundedForm,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".allowContactEmail" - {

    val fieldName = "allowContactEmail"
    val boundedForm = form.bind(
      feedbackValues - fieldName
    )
    val requiredKey = "feedback.allowContactEmail.error.required"

    "fail when contact is allowed and there is no contact email" in {
      boundedForm.errors.toSet mustEqual Set(FormError("", requiredKey))
    }
  }
}
