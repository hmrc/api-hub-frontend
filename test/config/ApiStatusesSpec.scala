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

import models.api.{Alpha, Beta, Deprecated, Live}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import play.api.Configuration

class ApiStatusesSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  import ApiStatusesSpec.*

  "ApiStatuses" - {
    "must correctly load valid configuration" in {
      val apiStatuses = ApiStatusesImpl(validConfiguration)
      val expected = Seq(alpha, beta, live, deprecated)

      apiStatuses.apiStatuses must contain theSameElementsInOrderAs expected
    }

    "must throw IllegalArgumentException when configuration contains an invalid status" in {
      val unknownStatus = "UNKNOWN"

      val configuration = Configuration(
        "apiStatuses" -> Map(
          buildConfig(unknownStatus, alpha)
        )
      )

      val thrown = the [IllegalArgumentException] thrownBy ApiStatusesImpl(configuration)

      thrown.getMessage mustBe s"$unknownStatus is not a known ApiStatus"
    }

    "must throw IllegalArgumentException when the statuses do no match the elements in ApiStatus" in {
      val configuration = Configuration(
        "apiStatuses" -> Map(
          buildConfig(alpha),
          buildConfig(beta),
          buildConfig(live)
        )
      )

      val thrown = the [IllegalArgumentException] thrownBy ApiStatusesImpl(configuration)

      thrown.getMessage mustBe "The API status configuration does not match the elements in ApiStatus"
    }

    "must throw IllegalArgumentException when the order is not contiguous" in {
      val configuration = Configuration(
        "apiStatuses" -> Map(
          buildConfig(alpha),
          buildConfig(beta),
          buildConfig(live),
          buildConfig(deprecated.copy(order = 0))
        )
      )

      val thrown = the [IllegalArgumentException] thrownBy ApiStatusesImpl(configuration)

      thrown.getMessage mustBe "The API status configuration does not have correct ordering"
    }

    "must return the correct description for an API status" in {
      val apiStatuses: ApiStatuses = ApiStatusesImpl(validConfiguration)

      forAll(allStatusConfigs)(apiStatusConfig =>
        apiStatuses.description(apiStatusConfig.apiStatus) mustBe apiStatusConfig.description
      )
    }

    "must return the correct CSS classes for an API status" in {
      val apiStatuses: ApiStatuses = ApiStatusesImpl(validConfiguration)

      forAll(allStatusConfigs)(apiStatusConfig =>
        apiStatuses.cssClasses(apiStatusConfig.apiStatus) mustBe apiStatusConfig.cssClasses
      )
    }
  }

}

private object ApiStatusesSpec extends TableDrivenPropertyChecks {

  val alpha: ApiStatusConfig = ApiStatusConfig(
    apiStatus = Alpha,
    description = "test-alpha-description",
    cssClasses = "test-alpha-css-classes",
    order = 1
  )

  val beta: ApiStatusConfig = ApiStatusConfig(
    apiStatus = Beta,
    description = "test-beta-description",
    cssClasses = "test-beta-css-classes",
    order = 2
  )

  val live: ApiStatusConfig = ApiStatusConfig(
    apiStatus = Live,
    description = "test-live-description",
    cssClasses = "test-live-css-classes",
    order = 3
  )

  val deprecated: ApiStatusConfig = ApiStatusConfig(
    apiStatus = Deprecated,
    description = "test-deprecated-description",
    cssClasses = "test-deprecated-css-classes",
    order = 4
  )

  val validConfiguration: Configuration = Configuration(
    "apiStatuses" -> Map(
      buildConfig(alpha),
      buildConfig(beta),
      buildConfig(live),
      buildConfig(deprecated)
    )
  )

  def buildConfig(name: String, apiStatusConfig: ApiStatusConfig): (String, Map[String, String]) = {
    name.toLowerCase -> Map(
      "apiStatus" -> name.toUpperCase,
      "description" -> apiStatusConfig.description,
      "cssClasses" -> apiStatusConfig.cssClasses,
      "order" -> apiStatusConfig.order.toString
    )
  }

  def buildConfig(apiStatusConfig: ApiStatusConfig): (String, Map[String, String]) = {
    buildConfig(apiStatusConfig.apiStatus.toString, apiStatusConfig)
  }

  val allStatusConfigs: TableFor1[ApiStatusConfig] = Table(
    "status",
    alpha,
    beta,
    live,
    deprecated
  )

}
