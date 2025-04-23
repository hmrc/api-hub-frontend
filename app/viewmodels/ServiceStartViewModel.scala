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

package viewmodels

import models.stats.DashboardStatistics
import models.user.UserModel

case class ServiceStartViewModel(
  user: Option[UserModel],
  dashboardStatistics: DashboardStatistics,
  exploreApisUrl: String,
  exploreFileTransfersUrl: String,
  documentationLinks: Seq[RelatedContentLink]
)

object ServiceStartViewModel {

  def apply(
    user: Option[UserModel],
    dashboardStatistics: DashboardStatistics,
    documentationLinks: Seq[RelatedContentLink]
  ): ServiceStartViewModel = {
    ServiceStartViewModel(
      user = user,
      dashboardStatistics = dashboardStatistics,
      exploreApisUrl = controllers.apis.routes.ExploreApisController.onPageLoad().url,
      exploreFileTransfersUrl = "/sdes-catalogue/file-types",
      documentationLinks = documentationLinks
    )
  }

}
