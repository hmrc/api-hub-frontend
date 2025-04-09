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

package fakes

import config.Domains
import models.api.{Domain, SubDomain}

object FakeDomains extends Domains {

  override val domains: Seq[Domain] = {
    Seq(
      data.domain1,
      data.domain2
    )
  }

  object data {
    val subDomain1_1: SubDomain = buildSubDomain(1, 1)
    val subDomain1_2: SubDomain = buildSubDomain(1, 2)
    val subDomain1_3: SubDomain = buildSubDomain(1, 3)
    val domain1: Domain = buildDomain(1, Seq(subDomain1_1, subDomain1_2, subDomain1_3))

    val subDomain2_1: SubDomain = buildSubDomain(2, 1)
    val subDomain2_2: SubDomain = buildSubDomain(2, 2)
    val domain2: Domain = buildDomain(2, Seq(subDomain2_1, subDomain2_2))

    val unknownDomainCode = "test-domain-code-unknown"
    val unknownSubDomainCode = "test-sub-domain-code-unknown"

    private def buildDomain(domainIndex: Int, subDomains: Seq[SubDomain]): Domain = {
      Domain(
        code = s"test-code-$domainIndex",
        description = s"Test Description $domainIndex",
        subDomains = subDomains
      )
    }

    private def buildSubDomain(domainIndex: Int, subDomainIndex: Int): SubDomain = {
      SubDomain(
        code = s"test-code-$domainIndex-$subDomainIndex",
        description = s"Test Description $domainIndex $subDomainIndex",
        basePath = s"/test/$domainIndex/$subDomainIndex"
      )
    }
  }

}
