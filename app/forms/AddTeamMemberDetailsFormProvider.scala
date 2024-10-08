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

import config.EmailDomains
import forms.mappings.Mappings
import models.application.TeamMember
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraints.{emailAddress, nonEmpty}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

import javax.inject.Inject

class AddTeamMemberDetailsFormProvider @Inject()(emailDomains: EmailDomains) extends Mappings {

  def apply(): Form[TeamMember] =
    Form(
      mapping(
        "email" -> text( "addTeamMemberDetails.email.invalid")
          .verifying(
            firstError(
              nonEmpty,
              emailAddress(errorMessage = "addTeamMemberDetails.email.invalid"),
              emailConstraint
            )
          )
      )(email => TeamMember(email.trim.toLowerCase))(o => Some(o.email))
    )

  private val emailConstraint = Constraint[String]("hmrcEmailConstraint")({
    email =>
      if (emailDomains.emailAddressHasValidDomain(email)) {
        Valid
      }
      else {
        Invalid(Seq(ValidationError("addTeamMemberDetails.email.invalid")))
      }
  })
}


