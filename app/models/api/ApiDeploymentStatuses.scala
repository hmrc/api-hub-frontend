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

package models.api

import models.application.EnvironmentName
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

sealed trait ApiDeploymentStatus {

  def environmentName: EnvironmentName

}

object ApiDeploymentStatus {

  case class Deployed(override val environmentName: EnvironmentName, version: String) extends ApiDeploymentStatus

  case class NotDeployed(override val environmentName: EnvironmentName) extends ApiDeploymentStatus

  case class Unknown(override val environmentName: EnvironmentName) extends ApiDeploymentStatus

  implicit val readDeploymentStatus: Reads[ApiDeploymentStatus] =
    ((__ \ "_type").read[String]
      ~ (__ \ "environmentName").read[EnvironmentName]
      ~ (__ \ "version").readNullable[String]
    )(deserialize.apply)

  private def deserialize(_type: String,
                          environmentName: EnvironmentName,
                          maybeVersion: Option[String]
                         ): ApiDeploymentStatus =
    (_type, maybeVersion) match {
      case (t, Some(version)) if t.contains(Deployed.getClass.getSimpleName.replace("$", "")) =>
        Deployed(environmentName, version)
      case (t, _) if t.contains(NotDeployed.getClass.getSimpleName.replace("$", "")) =>
        NotDeployed(environmentName)
      case _ => Unknown(environmentName)
    }

}

case class ApiDeploymentStatuses(statuses: Seq[ApiDeploymentStatus]) {

  def forEnvironment(environmentName: EnvironmentName): ApiDeploymentStatus = {
    // Throw an error as the configuration is out of sync between FE and BE
    statuses
      .find(_.environmentName == environmentName)
      .getOrElse(throw new IllegalArgumentException(s"No deployment status for environment $environmentName"))
  }

  def isDeployed: Boolean = statuses.collectFirst {
    case ApiDeploymentStatus.Deployed(_, _) => true
  }.getOrElse(false)

}

object ApiDeploymentStatuses {
  implicit val readApiDeploymentStatuses: Reads[ApiDeploymentStatuses] = Json.reads[ApiDeploymentStatuses]
}