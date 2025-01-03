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

import com.typesafe.config.ConfigFactory
import fakes.FakeEmailDomains
import models.application.{EnvironmentName, Primary, Secondary}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration

class HipEnvironmentsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  private val hoconConfig =
    """
      |hipEnvironments = {
      |    production = {
      |        id = "production",
      |        rank = 1,
      |        nameKey = "hipEnvironment.production.name",
      |        environmentName = "primary",
      |        isProductionLike = true
      |    },
      |    test = {
      |        id = "test",
      |        rank = 2,
      |        nameKey = "hipEnvironment.test.name",
      |        environmentName = "secondary",
      |        isProductionLike = false
      |    }
      |}
      |""".stripMargin
  private val hipEnvironmentsConfig = Configuration.apply(ConfigFactory.parseString(hoconConfig))
  private val expectedHipEnvironments = Seq(
    HipEnvironment(
      id = "production",
      rank = 1,
      nameKey = "hipEnvironment.production.name",
      environmentName = Primary,
      isProductionLike = true
    ),
    HipEnvironment(
      id = "test",
      rank = 2,
      nameKey = "hipEnvironment.test.name",
      environmentName = Secondary,
      isProductionLike = false
    ),
  )

  "HipEnvironments" - {
    "must load the expected environments" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      hipEnvironments.environments mustBe expectedHipEnvironments
    }
    "must retrieve the expected environments by environment name" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      forAll(Table(
        ("environmentName", "expectedEnvironment"),
        (Primary, expectedHipEnvironments.head),
        (Secondary, expectedHipEnvironments.last),
      )) { (environmentName: EnvironmentName, expectedEnvironment: HipEnvironment) =>
        hipEnvironments.forEnvironmentNameOptional(environmentName) mustBe Some(expectedEnvironment)
      }
    }

    "must retrieve the expected environments from url path parameter" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      forAll(Table(
        ("parameter", "expectedEnvironment"),
        ("primary", expectedHipEnvironments.head),
        ("production", expectedHipEnvironments.head),
        ("secondary", expectedHipEnvironments.last),
        ("test", expectedHipEnvironments.last),
      )) { (parameterValue: String, expectedEnvironment: HipEnvironment) =>
        hipEnvironments.forUrlPathParameter(parameterValue) mustBe expectedEnvironment
      }
    }
  }

}
