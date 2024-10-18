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

import config.Domains
import forms.mappings.Mappings
import models.myapis.produce.ProduceApiDomainSubdomain
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class ProduceApiDomainFormProvider @Inject()(domains: Domains) extends Mappings {

  def apply(): Form[ProduceApiDomainSubdomain] =
    Form(
      mapping(
        "domain" -> text("produceApiDomain.error.domainRequired"),
        "subDomain" -> text("produceApiDomain.error.subDomainRequired"),
      )(ProduceApiDomainSubdomain.apply)(o => Some(Tuple.fromProductTyped(o)))
        .verifying("produceApiDomain.error.invalidDomain", form => domains.domains.exists(_.code == form.domain))
        .verifying("produceApiDomain.error.invalidSubDomain", form => {
          val selectedDomain = domains.domains.find(_.code == form.domain)
          selectedDomain.exists(_.subDomains.exists(_.code == form.subDomain))
        })
    )
}
