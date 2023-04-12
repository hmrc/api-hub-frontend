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

import controllers.ScopeData
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text => play_text}

import javax.inject.Inject

class NewScopeFormProvider @Inject() extends Mappings {

  def apply(): Form[ScopeData] = Form(
    mapping(
      "scope-name" -> text("addScope.error.required").verifying(maxLength(100, "addScope.error.length")),
      "primary" -> optional(play_text),
      "secondary" -> optional(play_text)
    )(ScopeData.apply)(ScopeData.unapply).verifying(
      "addScope.error.required",
      scopeData => Seq(scopeData.primary, scopeData.secondary)
        .flatten[String]
        .nonEmpty))
}
