package forms.myapis.produce

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class ProduceApiEgressPrefixesFormProviderSpec extends StringFieldBehaviours {

  val form = new ProduceApiEgressPrefixesFormProvider()()

  ".prefixes" - {

    val fieldName = "prefixes"
    val requiredKey = "produceApiEgressPrefixes.error.prefixes.required"
    val lengthKey = "produceApiEgressPrefixes.error.prefixes.length"
    val maxLength = 100

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

  ".mappings" - {

    val fieldName = "mappings"
    val requiredKey = "produceApiEgressPrefixes.error.mappings.required"
    val lengthKey = "produceApiEgressPrefixes.error.mappings.length"
    val maxLength = 100

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
