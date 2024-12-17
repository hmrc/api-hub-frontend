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

package config

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}

case class HipEnvironment(
  id: String,
  rank: Int,                          // 1 is production
  nameKey: String,                    // Key for the environment's name in messages file
  isProductionLike: Boolean
) {
  def name()(implicit messages: play.api.i18n.Messages): String = messages(nameKey)
}

object HipEnvironment {

  implicit val hipEnvironmentConfigLoader: ConfigLoader[HipEnvironment] =
    (rootConfig: Config, path: String) => {
      val config = rootConfig.getConfig(path)

      HipEnvironment(
        id = config.getString("id"),
        rank = config.getInt("rank"),
        nameKey = config.getString("nameKey"),
        isProductionLike = config.getBoolean("isProductionLike")
      )
    }

}

trait HipEnvironments {

  def environments: Seq[HipEnvironment]

  def forId(environmentId: String): HipEnvironment = {
    environments
      .find(_.id == environmentId)
      .getOrElse(throw new IllegalArgumentException(s"No configuration for environment $environmentId"))
  }

  def production: HipEnvironment = {
    environments
      .find(_.isProductionLike)
      .getOrElse(throw new IllegalStateException("No production environment configured"))
  }

  def deployment: HipEnvironment = {
    environments
      .reverse
      .filterNot(_.isProductionLike)
      .headOption
      .getOrElse(throw new IllegalStateException("No deployment environment configured"))
  }

}

@Singleton
class HipEnvironmentsImpl @Inject(configuration: Configuration) extends HipEnvironments {

  override val environments: Seq[HipEnvironment] = {
    configuration
      .get[Map[String, HipEnvironment]]("hipEnvironments")
      .values
      .toSeq
      .sortBy(_.rank)
  }

}