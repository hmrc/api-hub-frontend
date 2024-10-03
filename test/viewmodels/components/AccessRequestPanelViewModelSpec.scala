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

package viewmodels.components

import generators.AccessRequestGenerator
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers

class AccessRequestPanelViewModelSpec extends AnyFreeSpec with Matchers with AccessRequestGenerator {

  private implicit val messages: Messages = Helpers.stubMessages()

  "consumerViewModel" - {
    "must build the correct model" in {
      val accessRequest = sampleAccessRequest()
      val actual = AccessRequestPanelViewModel.consumerViewModel(accessRequest)

      val expected = AccessRequestPanelViewModel(
        accessRequest = accessRequest,
        viewCall = controllers.application.routes.AccessRequestController.onPageLoad(accessRequest.id),
        viewMessage = "applicationHistory.viewRequest"
      )

      actual mustBe expected
    }
  }

  "adminViewModel" - {
    "must build the correct model" in {
      val accessRequest = sampleAccessRequest()
      val actual = AccessRequestPanelViewModel.adminViewModel(accessRequest)

      val expected = AccessRequestPanelViewModel(
        accessRequest = accessRequest,
        viewCall = controllers.admin.routes.AccessRequestController.onPageLoad(accessRequest.id),
        viewMessage = "accessRequests.headings.reviewRequest"
      )

      actual mustBe expected
    }
  }

}
