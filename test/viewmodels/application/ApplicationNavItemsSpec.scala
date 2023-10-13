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
import viewmodels.application.ApplicationPages._

class ApplicationNavItemsSpec extends SpecBase with Matchers with TableDrivenPropertyChecks {

  "ApplicationNavItems" - {
    "must return the correct list of nav items" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        val actual = ApplicationNavItems(FakeApplication, DetailsPage)
        val expected = Seq(
          ApplicationNavItem(
            DetailsPage,
            "Application details",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = true
          ),
          ApplicationNavItem(
            ApisPage,
            "Application APIs",
            controllers.application.routes.ApplicationApisController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          ApplicationNavItem(
            EnvironmentsAndCredentialsPage,
            "Environments and credentials",
            controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          ApplicationNavItem(
            ManageTeamMembersPage,
            "Manage team members",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          ApplicationNavItem(
            EditApplicationNamePage,
            "Edit application name",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          ApplicationNavItem(
            LeaveApplicationPage,
            "Leave application",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          ApplicationNavItem(
            DeleteApplicationPage,
            "Delete application",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          )
        )

        actual mustBe expected
      }
    }

    "must select the correct current page" in {
      val pages = Table(
        "page",
        DetailsPage,
        ApisPage,
        EnvironmentsAndCredentialsPage,
        ManageTeamMembersPage,
        EditApplicationNamePage,
        LeaveApplicationPage,
        DeleteApplicationPage
      )

      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        forAll(pages) {
          page =>
            val actual = ApplicationNavItems(FakeApplication, page)
              .filter(_.isCurrentPage)
              .map(_.page)

            actual mustBe Seq(page)
        }
      }
    }
  }

}
