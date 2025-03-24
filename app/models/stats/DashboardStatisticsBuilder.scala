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

import com.google.inject.{Inject, Singleton}
import config.Platforms
import models.api.{ApiDetail, IntegrationPlatformReport}

@Singleton
class DashboardStatisticsBuilder @Inject()(platforms: Platforms) {

  def build(report: Seq[IntegrationPlatformReport]): DashboardStatistics = {
    report
      .filter(_.integrationType == ApiDetail.IntegrationType.api)
      .foldLeft(DashboardStatistics(0, 0))(
        (dashboardStatistics, platform) =>
          DashboardStatistics(
            dashboardStatistics.totalApis + platform.count,
            dashboardStatistics.selfServiceApis + selfServiceApis(platform)
          )
      )

  }

  private def selfServiceApis(platform: IntegrationPlatformReport): Int = {
    if (platforms.isSelfServe(platform.platformType)) {
      platform.count
    }
    else {
      0
    }
  }

}
