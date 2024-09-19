package forms

import forms.behaviours.OptionFieldBehaviours
import forms.myapis.produce.ProduceApiHowToCreateFormProvider
import models.ProduceApiHowToCreate
import play.api.data.FormError

class ProduceApiHowToCreateFormProviderSpec extends OptionFieldBehaviours {

  val form = new ProduceApiHowToCreateFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "myApis.produce.howtocreate.error.required"

    behave like optionsField[ProduceApiHowToCreate](
      form,
      fieldName,
      validValues  = ProduceApiHowToCreate.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
