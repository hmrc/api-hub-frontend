/*
 * Copyright 2023 HM Revenue & Customs
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

package viewmodels.application

import base.SpecBase
import controllers.actions.FakeApplication
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.Helpers._
import viewmodels.SideNavItem
import viewmodels.application.DeletedApplicationSideNavPages.{DetailsPage, JsonViewPage}

class DeletedApplicationNavItemsSpec extends SpecBase with Matchers with TableDrivenPropertyChecks {

  "DeletedApplicationNavItems" - {
    "must return the correct list of nav items" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        val actual = DeletedApplicationNavItems(FakeApplication)
        val expected = Seq(
          SideNavItem(
            page = DetailsPage,
            title = "Application details",
            link = controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = true
          ),
          SideNavItem(
            page = JsonViewPage,
            title = "View as JSON",
            link = controllers.application.routes.ApplicationSupportController.onPageLoad(FakeApplication.id),
            isCurrentPage = false,
            opensInNewTab = true
          ),
        )

        actual mustBe expected
      }
    }

  }

}
