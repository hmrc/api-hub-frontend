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

package viewmodels

import models.api.ApiDeploymentStatuses

trait ApiViewModel {
  def domainDescription: Option[String]
  def subDomainDescription: Option[String]
  def hodDescriptions: Seq[String]
  def platformDescription: String
}

case class SelfServeApiViewModel(
  domainDescription: Option[String],
  subDomainDescription: Option[String],
  hodDescriptions: Seq[String],
  platformDescription: String,
  teamName: Option[String],
  deploymentStatuses: ApiDeploymentStatuses
) extends ApiViewModel

case class NonSelfServeApiViewModel(
  domainDescription: Option[String],
  subDomainDescription: Option[String],
  hodDescriptions: Seq[String],
  platformDescription: String,
  contactEmail: ApiContactEmail
) extends ApiViewModel

trait ApiContactEmail {
  def email: String
}
case class ApiTeamContactEmail(email: String) extends ApiContactEmail
case class HubSupportContactEmail(email: String) extends ApiContactEmail
