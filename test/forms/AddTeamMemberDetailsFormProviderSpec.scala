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

import forms.behaviours.StringFieldBehaviours
import models.application.TeamMember
import play.api.data.FormError

class AddTeamMemberDetailsFormProviderSpec extends StringFieldBehaviours {

  val invalidKey = "addTeamMemberDetails.email.invalid"

  val form = new AddTeamMemberDetailsFormProvider()()

  ".email" - {

    val fieldName = "email"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      arbitraryHmrcEmail.arbitrary
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, invalidKey)
    )

    "must not accept non-HMRC email addresses" in {
      forAll(arbitraryNonHmrcEmail.arbitrary -> "invalidEmail") {
        email: String =>
          val result = form.bind(Map(fieldName -> email)).apply(fieldName)
          result.errors must contain(FormError(fieldName, invalidKey))
      }

    }

    "must lower-case the input value" in {
      val email = "Ab.Cd@HmRc.GoV.Uk"
      form.bind(Map(fieldName -> email)).fold(
        errors => fail(errors.toString),
        identity
      ) mustBe TeamMember(email.trim.toLowerCase())
    }
  }

}
