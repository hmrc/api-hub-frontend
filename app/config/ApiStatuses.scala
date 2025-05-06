/*
 * Copyright 2025 HM Revenue & Customs
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
import com.typesafe.config.{Config, ConfigException}
import models.api.ApiStatus
import play.api.{ConfigLoader, Configuration}

case class ApiStatusConfig(apiStatus: ApiStatus, description: String)

object ApiStatusConfig {

  implicit val apiStatusesConfigLoader: ConfigLoader[ApiStatusConfig] =
    (rootConfig: Config, path: String) => {
      val config = rootConfig.getConfig(path)

      val name = config.getString("apiStatus").toUpperCase
      val description = config.getString("description")

      ApiStatus.values.find(_.toString.equals(name)) match {
        case Some(apiStatus) => ApiStatusConfig(
          apiStatus = apiStatus,
          description = description
        )
        case _ =>
          throw new ConfigException.BadValue(path, s"$name is not a known ApiStatus")
      }
    }
}

trait ApiStatuses {

  protected def apiStatuses: Seq[ApiStatusConfig]

  def description(apiStatus: ApiStatus): String = {
    apiStatuses.find(_.apiStatus.equals(apiStatus)) match {
      case Some(apiStatus) => apiStatus.description
      case _ => throw new IllegalArgumentException(s"No API status configuration found for API status $apiStatus")
    }
  }

}

@Singleton
class ApiStatusesImpl @Inject(configuration: Configuration)() extends ApiStatuses {

  override protected val apiStatuses: Seq[ApiStatusConfig] = {
    configuration
      .get[Map[String, ApiStatusConfig]]("apiStatuses")
      .values
      .toSeq
  }

}
