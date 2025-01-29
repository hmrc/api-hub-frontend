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
      isProductionLike = true
    ),
    HipEnvironment(
      id = "test",
      rank = 2,
      nameKey = "hipEnvironment.test.name",
      isProductionLike = false
    ),
  )

  "HipEnvironments" - {
    "must load the expected environments" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      hipEnvironments.environments mustBe expectedHipEnvironments
    }
    "must retrieve the expected environments by environment id" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      forAll(Table(
        ("environmentName", "expectedEnvironment"),
        ("production", expectedHipEnvironments.head),
        ("test", expectedHipEnvironments.last),
      )) { (environmentId: String, expectedEnvironment: HipEnvironment) =>
        hipEnvironments.forEnvironmentIdOptional(environmentId) mustBe Some(expectedEnvironment)
      }
    }

    "must retrieve the expected environments from url path parameter" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      forAll(Table(
        ("parameter", "expectedEnvironment"),
        ("production", expectedHipEnvironments.head),
        ("test", expectedHipEnvironments.last),
      )) { (parameterValue: String, expectedEnvironment: HipEnvironment) =>
        hipEnvironments.forUrlPathParameter(parameterValue) mustBe expectedEnvironment
      }
    }

    "must try and find the production environment" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      hipEnvironments.productionHipEnvironment.id mustBe "production"
    }

    "must try and find the 'deployment' environment" in {
      val hipEnvironments = HipEnvironmentsImpl(hipEnvironmentsConfig)

      hipEnvironments.deploymentHipEnvironment.id mustBe "test"
    }

    "promotionEnvironment" - {
      val hipMultiEnvironmentsConfig = Configuration.apply(ConfigFactory.parseString("""
         |hipEnvironments = {
         |    test = {
         |        id = "test",
         |        rank = 3,
         |        nameKey = "hipEnvironment.test.name",
         |        isProductionLike = false
         |    },
         |    production = {
         |        id = "production",
         |        rank = 1,
         |        nameKey = "hipEnvironment.production.name",
         |        isProductionLike = true
         |    },
         |    dev = {
         |        id = "dev",
         |        rank = 4,
         |        nameKey = "hipEnvironment.dev.name",
         |        isProductionLike = false
         |    },
         |    preprod = {
         |        id = "preprod",
         |        rank = 2,
         |        nameKey = "hipEnvironment.preprod.name",
         |        isProductionLike = false
         |    }
         |}
         |""".stripMargin))
        val hipMultiEnvironments = HipEnvironmentsImpl(hipMultiEnvironmentsConfig)
        val prodEnvironment = hipMultiEnvironments.forEnvironmentId("production")
        val preprodEnvironment = hipMultiEnvironments.forEnvironmentId("preprod")
        val testEnvironment = hipMultiEnvironments.forEnvironmentId("test")
        val devEnvironment = hipMultiEnvironments.forEnvironmentId("dev")

      "production has no promotion environment" in {
        hipMultiEnvironments.promotionEnvironment(prodEnvironment) mustBe None
      }
      "pre-production promotes to production" in {
        hipMultiEnvironments.promotionEnvironment(preprodEnvironment) mustBe Some(prodEnvironment)
      }
      "test promotes to pre-production" in {
        hipMultiEnvironments.promotionEnvironment(testEnvironment) mustBe Some(preprodEnvironment)
      }
      "dev promotes to test" in {
        hipMultiEnvironments.promotionEnvironment(devEnvironment) mustBe Some(testEnvironment)
      }
    }

  }

}
