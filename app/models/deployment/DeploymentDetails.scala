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

package models.deployment

import play.api.libs.json.{Format, Json}

case class DeploymentDetails (
  description: Option[String],
  status: Option[String],
  domain: Option[String],
  subDomain: Option[String],
  hods: Option[Seq[String]],
  egressMappings: Option[Seq[EgressMapping]],
  prefixesToRemove: Option[Seq[String]],
  egress: Option[String]
) {
  val hasEgress: Boolean = egress.exists(_ != DeploymentDetails.egressFallback)
}

object DeploymentDetails {
  private val egressFallback: String = "null-egress"
  implicit val formatDeploymentDetails: Format[DeploymentDetails] = Json.format[DeploymentDetails]

}
