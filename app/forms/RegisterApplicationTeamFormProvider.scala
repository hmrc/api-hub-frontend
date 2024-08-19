package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.RegisterApplicationTeam

class RegisterApplicationTeamFormProvider @Inject() extends Mappings {

  def apply(): Form[RegisterApplicationTeam] =
    Form(
      "value" -> enumerable[RegisterApplicationTeam]("registerApplicationTeam.error.required")
    )
}
