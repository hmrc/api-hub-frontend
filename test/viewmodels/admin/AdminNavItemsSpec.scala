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

package viewmodels.admin

import base.SpecBase
import controllers.actions.{FakeApprover, FakeSupporter}
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.Helpers.running
import viewmodels.SideNavItem
import viewmodels.admin.AdminSideNavPages.{AccessRequestsPage, GetUsersPage, ManageApisPage, ManageApplicationsPage, ManageTeamsPage}
import viewmodels.admin.AdminSideNavPages.TeamMigrationPage

class AdminNavItemsSpec extends SpecBase with Matchers with TableDrivenPropertyChecks {

  import AdminNavItemsSpec._

  "class AdminNavItems" - {
    "must return the correct list of nav items for support" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)
        val actual = AdminNavItems(FakeSupporter, AccessRequestsPage)
        val expected = Seq(manageApplicationsNavItem(), manageApisNavItem(), manageTeamsNavItem(), teamMigrationNavItem(), getUsersNavItem(), accessRequestsNavItem())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for an approver" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)
        val actual = AdminNavItems(FakeApprover, AccessRequestsPage)
        val expected = Seq(accessRequestsNavItem())

        actual mustBe expected
      }
    }

    "must select the correct current page" in {
      val pages = Table(
        "Page",
        ManageApplicationsPage,
        AccessRequestsPage
      )

      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        forAll(pages) {
          page =>
            val actual = AdminNavItems(FakeSupporter, page)
              .filter(_.isCurrentPage)
              .map(_.page)

            actual mustBe Seq(page)
        }
      }

    }
  }

}

object AdminNavItemsSpec {

  private def accessRequestsNavItem(): SideNavItem = {
    SideNavItem(
      page = AccessRequestsPage,
      title = "API requests",
      link = controllers.admin.routes.AccessRequestsController.onPageLoad(),
      isCurrentPage = true
    )
  }

  private def manageApplicationsNavItem(): SideNavItem = {
    SideNavItem(
      page = ManageApplicationsPage,
      title = "Manage applications",
      link = controllers.admin.routes.ManageApplicationsController.onPageLoad(),
      isCurrentPage = false
    )
  }

  private def manageApisNavItem(): SideNavItem = {
    SideNavItem(
      page = ManageApisPage,
      title = "Manage APIs",
      link = controllers.admin.routes.ManageApisController.onPageLoad(),
      isCurrentPage = false
    )
  }

  private def getUsersNavItem(): SideNavItem = {
    SideNavItem(
      page = GetUsersPage,
      title = "View users",
      link = controllers.admin.routes.GetUsersController.onPageLoad(),
      isCurrentPage = false
    )
  }

  private def manageTeamsNavItem(): SideNavItem = {
    SideNavItem(
      page = ManageTeamsPage,
      title = "Manage teams",
      link = controllers.admin.routes.ManageTeamsController.onPageLoad(),
      isCurrentPage = false
    )
  }

  private def teamMigrationNavItem(): SideNavItem = {
    SideNavItem(
      page = TeamMigrationPage,
      title = "Team migration",
      link = controllers.admin.routes.TeamMigrationController.onPageLoad(),
      isCurrentPage = false
    )
  }

}
