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
import models.myapis.produce.ProduceApiUploadedWiremockFile
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class ProduceApiUploadWiremockFormProvider @Inject() extends Mappings {

  def apply(): Form[ProduceApiUploadedWiremockFile] =
    Form(
      mapping(
        "fileName" -> text("produceApiUploadWiremock.error.required"),
        "fileContents" -> text("")
      )(ProduceApiUploadedWiremockFile.apply)(o => Some(Tuple.fromProductTyped(o)))
    )
}
