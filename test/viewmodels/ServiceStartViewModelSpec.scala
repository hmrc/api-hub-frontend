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

import controllers.actions.FakeUser
import models.stats.DashboardStatistics
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ServiceStartViewModelSpec extends AnyFreeSpec with Matchers {

  "apply" - {
    "must return the correct view model" in {
      val dashboardStatistics = DashboardStatistics(12, 34)

      val links = Seq(
        RelatedContentLink(
          description =  "test-link-description-1",
          url = "test-link-url-1"
        ),
        RelatedContentLink(
          description =  "test-link-description-2",
          url = "test-link-url-2"
        )
      )

      val expected = ServiceStartViewModel(
        user = Some(FakeUser),
        dashboardStatistics = dashboardStatistics,
        exploreApisUrl = controllers.routes.ExploreApisController.onPageLoad().url,
        exploreFileTransfersUrl = "/sdes-catalogue/file-types",
        documentationLinks = links
      )

      val actual = ServiceStartViewModel(Some(FakeUser), dashboardStatistics, links)

      actual mustBe expected
    }
  }

}
