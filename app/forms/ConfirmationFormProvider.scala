package forms

import com.google.inject.Inject
import forms.mappings.Mappings
import models.Confirmation
import play.api.data.Form
import play.api.data.Forms.set

class ConfirmationFormProvider @Inject() extends Mappings {

  def apply(errorKey: String): Form[Confirmation] =
    Form(
      "value" -> set(enumerable[Confirmation](errorKey)).verifying(nonEmptySet(errorKey))
    )

}
