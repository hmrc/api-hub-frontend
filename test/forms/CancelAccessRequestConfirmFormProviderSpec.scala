package forms

import forms.application.cancelaccessrequest.CancelAccessRequestConfirmFormProvider
import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class CancelAccessRequestConfirmFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "cancelAccessRequestConfirm.error.required"
  val invalidKey = "error.boolean"

  val form = new CancelAccessRequestConfirmFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
