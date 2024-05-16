/*
 * Copyright 2023 HM Revenue & Customs
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

package models.api

import models.Lens

object ApiDetailLenses {

  val apiDetailDomain: Lens[ApiDetail, Option[String]] =
    Lens[ApiDetail, Option[String]](
      get = _.domain,
      set = (apiDetail, domain) => apiDetail.copy(domain = domain)
    )

  val apiDetailSubDomain: Lens[ApiDetail, Option[String]] =
    Lens[ApiDetail, Option[String]](
      get = _.subDomain,
      set = (apiDetail, subDomain) => apiDetail.copy(subDomain = subDomain)
    )

  implicit class ApiDetailLensOps(apiDetail: ApiDetail) {

    def getRequiredScopeNames: Set[String] = {
      apiDetail
        .endpoints
        .flatMap(_.methods)
        .flatMap(_.scopes)
        .toSet
    }

    def setDomain(domainCode: Option[String]): ApiDetail =
      apiDetailDomain.set(apiDetail, domainCode)

    def setDomain(domainCode: String): ApiDetail =
      setDomain(Some(domainCode))

    def setDomain(domain: Domain): ApiDetail =
      setDomain(domain.code)

    def setSubDomain(subDomainCode: Option[String]): ApiDetail =
      apiDetailSubDomain.set(apiDetail, subDomainCode)

    def setSubDomain(subDomainCode: String): ApiDetail =
      setSubDomain(Some(subDomainCode))

    def setSubDomain(subDomain: SubDomain): ApiDetail =
      setSubDomain(subDomain.code)

  }

}
