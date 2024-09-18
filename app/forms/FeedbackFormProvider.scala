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

import forms.mappings.Mappings
import models.Feedback
import models.Feedback.FeedbackType
import play.api.data.Form
import play.api.data.Forms.{email, mapping, nonEmptyText, optional, set}

import javax.inject.Inject

class FeedbackFormProvider @Inject() extends Mappings {

  def apply(): Form[Feedback] =
    Form[Feedback](
      mapping(
        "type" -> text("feedback.type.error.required")
          .transform(FeedbackType.valueOf, _.toString),
        "otherType" -> optional(nonEmptyText(minLength = 1)),
        "rate" -> int("feedback.rate.error.required"),
        "comments" -> text("feedback.comments.error.required"),
        "allowContact" -> boolean("feedback.allowContact.error.required"),
        "allowContactEmail" -> optional(email)
      )(Feedback.apply)(o => Some(Tuple.fromProductTyped(o)))
        .verifying("feedback.otherType.error.required", f => f.`type` != FeedbackType.Other || f.otherType.isDefined)
        .verifying("feedback.allowContactEmail.error.required", f => !f.allowContact || f.email.isDefined)
    )
}
