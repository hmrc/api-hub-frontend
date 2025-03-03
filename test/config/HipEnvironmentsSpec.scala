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
import org.mockito.ArgumentMatchers.{any, matches}
import org.mockito.Mockito.{mock, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import services.ApiHubService
import matchers.CustomMatchers
import org.scalatest.OptionValues

import scala.concurrent.Future

class HipEnvironmentsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks with MockitoSugar with CustomMatchers with OptionValues {

  private val apiHubService = mock[ApiHubService]
  private val frontendAppConfig = mock[FrontendAppConfig]
  when(frontendAppConfig.hipEnvironmentsLookupTimeoutSeconds).thenReturn(1)
  
  private val production = DefaultHipEnvironment(
      id = "production",
      rank = 1,
      isProductionLike = true,
      promoteTo = None
  )
  private val test = DefaultHipEnvironment(
      id = "test",
      rank = 2,
      isProductionLike = false,
      promoteTo = Some(production)
  )

  private val expectedHipEnvironments = Seq(
      production,
      test,
  )

  private val productionBaseEnv = BaseHipEnvironment(
    id = "production",
    rank = 1,
    isProductionLike = true,
    promoteTo = None
  )

  private val testBaseEnv = BaseHipEnvironment(
    id = "test",
    rank = 2,
    isProductionLike = false,
    promoteTo = Some("production")
  )

  when(apiHubService.listEnvironments()(any()))
    .thenReturn(Future.successful(ShareableHipConfig(Seq(productionBaseEnv, testBaseEnv), "production", "test")))

  "HipEnvironments" - {
    "must load the expected environments" in {
      val hipEnvironments = HipEnvironmentsImpl(apiHubService, frontendAppConfig)
      hipEnvironments.environments.size mustBe 2
      hipEnvironments.environments.head must matchHipEnvironment(production)
      hipEnvironments.environments.last must matchHipEnvironment(test)
    }
    "must retrieve the expected environments by environment id" in {
      val hipEnvironments = HipEnvironmentsImpl(apiHubService, frontendAppConfig)

      forAll(Table(
        ("environmentName", "expectedEnvironment"),
        ("production", production),
        ("test", test),
      )) { (environmentId: String, expectedEnvironment: DefaultHipEnvironment) =>
        hipEnvironments.forEnvironmentIdOptional(environmentId).get must matchHipEnvironment(expectedEnvironment)
      }
    }

    "must retrieve the expected environments from url path parameter" in {
      val hipEnvironments = HipEnvironmentsImpl(apiHubService, frontendAppConfig)

      forAll(Table(
        ("parameter", "expectedEnvironment"),
        ("production", production),
        ("test", test),
      )) { (parameterValue: String, expectedEnvironment: DefaultHipEnvironment) =>
        hipEnvironments.forUrlPathParameter(parameterValue).value must matchHipEnvironment(expectedEnvironment)
      }
    }

    "must try and find the production environment" in {
      val hipEnvironments = HipEnvironmentsImpl(apiHubService, frontendAppConfig)

      hipEnvironments.production.id mustBe "production"
    }

    "must try and find the 'deployment' environment" in {
      val hipEnvironments = HipEnvironmentsImpl(apiHubService, frontendAppConfig)

      hipEnvironments.
        deployTo.id mustBe "test"
    }

  }

}
