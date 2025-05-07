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
import com.typesafe.config.Config
import models.api.ApiStatus
import play.api.{ConfigLoader, Configuration}

case class ApiStatusConfig(
  apiStatus: ApiStatus,
  description: String,
  cssClasses: String,
  order: Int
)

object ApiStatusConfig {

  implicit val apiStatusesConfigLoader: ConfigLoader[ApiStatusConfig] =
    (rootConfig: Config, path: String) => {
      val config = rootConfig.getConfig(path)

      val apiStatus = config.getString("apiStatus").toUpperCase

      ApiStatus.values.find(_.toString.equals(apiStatus)) match {
        case Some(apiStatus) =>
          ApiStatusConfig(
            apiStatus = apiStatus,
            description = config.getString("description"),
            cssClasses = config.getString("cssClasses"),
            order = config.getInt("order")
          )
        case _ =>
          throw new IllegalArgumentException(s"$apiStatus is not a known ApiStatus")
      }
    }
}

trait ApiStatuses {

  def apiStatuses: Seq[ApiStatusConfig]

  def description(apiStatus: ApiStatus): String = {
    findStatus(apiStatus).description
  }

  def cssClasses(apiStatus: ApiStatus): String = {
    findStatus(apiStatus).cssClasses
  }

  private def findStatus(apiStatus: ApiStatus): ApiStatusConfig = {
    apiStatuses.find(_.apiStatus.equals(apiStatus)) match {
      case Some(apiStatus) => apiStatus
      case _ => throw new IllegalArgumentException(s"No API status configuration found for API status $apiStatus")
    }
  }
}

@Singleton
class ApiStatusesImpl @Inject(configuration: Configuration)() extends ApiStatuses {

  override val apiStatuses: Seq[ApiStatusConfig] = {
    val statuses = configuration
      .get[Map[String, ApiStatusConfig]]("apiStatuses")
      .values
      .toSeq
      .sortBy(_.order)

    validate(statuses)

    statuses
  }

  private def validate(apiStatuses: Seq[ApiStatusConfig]): Unit = {
    if (!apiStatuses.map(_.apiStatus).toSet.equals(ApiStatus.values.toSet)) {
      throw new IllegalArgumentException("The API status configuration does not match the elements in ApiStatus")
    }

    if (!apiStatuses.map(_.order).equals(1 to apiStatuses.size)) {
      throw new IllegalArgumentException("The API status configuration does not have correct ordering")
    }
  }

}
