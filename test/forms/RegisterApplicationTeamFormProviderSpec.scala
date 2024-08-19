package forms

import forms.behaviours.OptionFieldBehaviours
import models.RegisterApplicationTeam
import play.api.data.FormError

class RegisterApplicationTeamFormProviderSpec extends OptionFieldBehaviours {

  val form = new RegisterApplicationTeamFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "registerApplicationTeam.error.required"

    behave like optionsField[RegisterApplicationTeam](
      form,
      fieldName,
      validValues  = RegisterApplicationTeam.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
