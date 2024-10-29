/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.myapis.produce

import forms.mappings.Mappings
import models.myapis.produce.ProduceApiChooseEgress
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class ProduceApiChooseEgressFormProvider @Inject() extends Mappings {

  def apply(): Form[ProduceApiChooseEgress] = Form(
      mapping(
        "selectEgress" -> text()
          .transform(mapToEgressId, mapToEgressInput)
          .verifying("myApis.produce.selectegress.error.required", _.isDefined),
        "egressPrefix" -> boolean("myApis.produce.egressprefix.error.required")
      )(ProduceApiChooseEgress.apply)(o => Some(Tuple.fromProductTyped(o)))
    )
  
  private def mapToEgressId(input: String): Option[String] = {
    if (input.trim.toLowerCase.equals("unassigned")) {
      None
    }
    else {
      Some(input)
    }
  }

  private def mapToEgressInput(maybeEgress: Option[String]): String = {
    maybeEgress.getOrElse("unassigned")
  }

  private def mapToEgressPrefix(input: String): Option[String] = {
    if (input.isBlank) {
      None
    }
    else {
      Some(input)
    }
  }

  private def mapToEgressPrefixInput(maybeEgressPrefix: Option[String]): String = {
    maybeEgressPrefix.getOrElse("")
  }
}
