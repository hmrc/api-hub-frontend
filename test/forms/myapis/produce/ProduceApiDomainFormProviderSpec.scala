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
import forms.behaviours.OptionFieldBehaviours
import models.api.{Domain, SubDomain}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.FormError

class ProduceApiDomainFormProviderSpec extends OptionFieldBehaviours with MockitoSugar {

  private val validDomains = (1 to 5).map(d =>
    Domain(s"code$d", s"domain$d", (1 to 5).map(s =>SubDomain(s"code$d$s", s"subdomain$d$s", s"/$d/$s")))
  )
  private val domains = mock[Domains]
  when(domains.domains).thenReturn(validDomains)

  val form = new ProduceApiDomainFormProvider(domains)()

  ".domain" - {
    val fieldName = "domain"
    val requiredKey = "produceApiDomain.error.domainRequired"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      oneOf(validDomains.map(_.code))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".subDomain" - {
    val fieldName = "subDomain"
    val requiredKey = "produceApiDomain.error.subDomainRequired"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      oneOf(validDomains.head.subDomains.map(_.code))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
