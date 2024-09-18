package forms.myapis.produce

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class ProduceApiEnterOasFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("produceApiEnterOas.error.required")
    )
}
