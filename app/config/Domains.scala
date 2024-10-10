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

package config

import com.google.inject.{Inject, Singleton}
import models.api.{ApiDetail, ApiDetailSummary, Domain, SubDomain}
import play.api.Configuration

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait Domains {

  def domains: Seq[Domain]

  def getDomain(domainCode: String): Option[Domain] = {
    domains
      .find(domain => normalise(domain.code).equals(normalise(domainCode)))
  }

  def getDomainDescription(domainCode: String): String = {
    getDomain(domainCode)
      .map(_.description)
      .getOrElse(domainCode)
  }

  def getDomainDescription(domainCode: Option[String]): Option[String] = {
    domainCode.map(getDomainDescription)
  }

  def getDomainDescription(apiDetail: ApiDetail): Option[String] = {
    getDomainDescription(apiDetail.domain)
  }

  def getDomainDescription(apiDetail: ApiDetailSummary): Option[String] = {
    getDomainDescription(apiDetail.domain)
  }

  def getSubDomain(domainCode: String, subDomainCode: String): Option[SubDomain] = {
    getDomain(domainCode)
      .flatMap(
        domain =>
          domain.subDomains
            .find(subDomain => normalise(subDomain.code).equals(normalise(subDomainCode)))
      )
  }

  def getSubDomainDescription(domainCode: String, subDomainCode: String): String = {
    getSubDomain(domainCode, subDomainCode)
      .map(_.description)
      .getOrElse(subDomainCode)
  }

  def getSubDomainDescription(domainCode: Option[String], subDomainCode: Option[String]): Option[String] = {
    (domainCode, subDomainCode) match {
      case (Some(domainCode), Some(subDomainCode)) => Some(getSubDomainDescription(domainCode, subDomainCode))
      case (None, Some(subDomainCode)) => Some(subDomainCode)
      case _ => None
    }
  }

  def getSubDomainDescription(apiDetail: ApiDetail): Option[String] = {
    getSubDomainDescription(apiDetail.domain, apiDetail.subDomain)
  }

  def getSubDomainDescription(apiDetail: ApiDetailSummary): Option[String] =
    getSubDomainDescription(apiDetail.domain, apiDetail.subDomain)

  private def normalise(s: String): String = {
    s.trim.toLowerCase()
  }

}

@Singleton
class DomainsImpl @Inject()(configuration: Configuration) extends Domains {

  override val domains: Seq[Domain] = configuration.get[Seq[Configuration]]("domains").map {
    configuration => {
      val config = configuration.underlying
      Domain(
        code = config.getString("code"),
        description = config.getString("description"),
        subDomains = config.getConfig("subdomains").entrySet.asScala.map(
          e => SubDomain(e.getKey, e.getValue.unwrapped().toString)
        ).toSeq
      )
    }
  }

}
