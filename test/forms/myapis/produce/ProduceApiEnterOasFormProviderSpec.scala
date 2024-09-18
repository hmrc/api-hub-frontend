package forms.myapis.produce

import forms.behaviours.StringFieldBehaviours
import forms.myapis.produce.ProduceApiEnterOasFormProvider
import play.api.data.FormError

class ProduceApiEnterOasFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "produceApiEnterOas.error.required"
  val lengthKey = "produceApiEnterOas.error.length"
  val maxLength = 100

  val form = new ProduceApiEnterOasFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
