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
import models.application.{Development, EnvironmentName, PreProduction, Production, Test}
import play.api.Logging
import play.api.libs.json.{Format, Json}

case class EnvironmentConfig(environmentName: EnvironmentName, on: Boolean)

object EnvironmentConfig {

  implicit val formatEnvironmentConfig: Format[EnvironmentConfig] = Json.format[EnvironmentConfig]

}

case class EnvironmentsConfig(
  production: EnvironmentConfig,
  preProduction: EnvironmentConfig,
  test: EnvironmentConfig,
  development: EnvironmentConfig
)

object EnvironmentsConfig {

  implicit val formatEnvironmentsConfig: Format[EnvironmentsConfig] = Json.format[EnvironmentsConfig]

}

trait Environments {

  def config: EnvironmentsConfig

}

@Singleton
class EnvironmentsImpl @Inject() extends Environments with Logging {

  private val environmentsConfig = EnvironmentsConfig(
    production = EnvironmentConfig(Production, on = true),
    preProduction = EnvironmentConfig(PreProduction, on = true),
    test = EnvironmentConfig(Test, on = true),
    development = EnvironmentConfig(Development, on = true)
  )

  logger.info(s"Environment configuration${System.lineSeparator()}${Json.prettyPrint(Json.toJson(config))}")

  override def config: EnvironmentsConfig = environmentsConfig

}
