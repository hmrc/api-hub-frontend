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
import models.myapis.produce.ProduceApiEgressPrefixes
import play.api.data.{Form, Forms}
import play.api.data.Forms.*

import javax.inject.Inject
import ProduceApiEgressPrefixes.mappingSeparator

class ProduceApiEgressPrefixesFormProvider @Inject() extends Mappings {

   def apply(): Form[ProduceApiEgressPrefixes] = Form(
     mapping(
      "prefixes" -> Forms.seq(text("produceApiEgressPrefix.prefixes.error.missing"))
          .verifying("produceApiEgressPrefix.prefixes.error.startWithSlash", _.forall(_.startsWith("/")))
          .transform(_.map(_.trim), identity),

      "mappings" -> Forms.seq(text("produceApiEgressPrefix.mappings.error.missing"))
          .verifying("produceApiEgressPrefix.mappings.error.separator", _.forall(_.split(mappingSeparator).length == 2))
          .verifying("produceApiEgressPrefix.mappings.error.startWithSlash", _.flatMap(_.split(mappingSeparator)).forall(_.startsWith("/")))
          .transform(_.map(_.trim), identity),

    )(ProduceApiEgressPrefixes.apply)(ProduceApiEgressPrefixes.unapply)
   )
 }
