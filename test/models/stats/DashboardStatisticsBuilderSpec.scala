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

package models.stats

import fakes.FakePlatforms
import models.api.{ApiDetail, IntegrationPlatformReport}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class DashboardStatisticsBuilderSpec extends AnyFreeSpec with Matchers {

  "build" - {
    "must build the correct statistics" in {
      val report = Seq(
        IntegrationPlatformReport(
          platformType = "HIP",
          integrationType = ApiDetail.IntegrationType.api,
          count = 12
        ),
        IntegrationPlatformReport(
          platformType = "HIP",
          integrationType = "NOT-API",
          count = 34
        ),
        IntegrationPlatformReport(
          platformType = "NOT-HIP",
          integrationType = ApiDetail.IntegrationType.api,
          count = 56
        )
      )

      val expected = DashboardStatistics(
        totalApis = 12 + 56,
        selfServiceApis = 12
      )

      val builder = new DashboardStatisticsBuilder(FakePlatforms)

      builder.build(report) mustBe expected
    }
  }

}
