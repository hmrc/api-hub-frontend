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
import controllers.actions.{FakeApplication, FakeSupporter, FakeUser}
import models.application.ApplicationLenses._
import models.user.UserModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.TestHelpers
import viewmodels.SideNavItem
import viewmodels.application.ApplicationSideNavPages._

class ApplicationNavItemsSpec extends SpecBase with Matchers with TestHelpers with TableDrivenPropertyChecks {

  "ApplicationNavItems" - {
    "must return the correct list of nav items" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        val actual = ApplicationNavItems(Some(FakeSupporter), FakeApplication, DetailsPage)
        val expected = Seq(
          SideNavItem(
            DetailsPage,
            "Application details",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = true
          ),
          SideNavItem(
            ApisPage,
            "Application APIs",
            controllers.application.routes.ApplicationApisController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItem(
            EnvironmentsAndCredentialsPage,
            "Environments and credentials",
            controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItem(
            ManageTeamMembersPage,
            "Manage team members",
            controllers.application.routes.ManageTeamMembersController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItem(
            DeleteApplicationPage,
            "Delete application",
            controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItem(
            page = ViewAsJsonApplicationPage,
            title = "View as JSON",
            link = controllers.application.routes.ApplicationSupportController.onPageLoad(FakeApplication.id),
            isCurrentPage = false,
            opensInNewTab = true
          ),
          SideNavItem(
            page = ChangeOwningTeamPage,
            title = "Change owning team",
            link = controllers.application.routes.UpdateApplicationTeamController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItem(
            page = ApplicationHistoryPage,
            title = "Application history",
            link = controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(FakeApplication.id),
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
        DeleteApplicationPage
      )

      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        forAll(pages) {
          page =>
            val actual = ApplicationNavItems(None, FakeApplication, page)
              .filter(_.isCurrentPage)
              .map(_.page)

            actual mustBe Seq(page)
        }
      }
    }

    "must not return the Manage team members item for applications with a global team" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        val actual = ApplicationNavItems(Some(FakeSupporter), FakeApplication.setTeamId("test-team-id"), DetailsPage)
          .filter(_.page.equals(ManageTeamMembersPage))

        actual mustBe empty
      }
    }

    "must not display the View as JSON item for non-support users" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val playApplication = applicationBuilder(None).build()

        running(playApplication) {
          implicit val implicitMessages: Messages = messages(playApplication)

          val actual = ApplicationNavItems(Some(user), FakeApplication.setTeamId("test-team-id"), DetailsPage)
            .filter(_.page.equals(ViewAsJsonApplicationPage))

          actual mustBe empty
        }
      }
    }

    "must not display the Application history item for non-support users" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val playApplication = applicationBuilder(None).build()

        running(playApplication) {
          implicit val implicitMessages: Messages = messages(playApplication)

          val actual = ApplicationNavItems(Some(FakeUser), FakeApplication.setTeamId("test-team-id"), DetailsPage)
            .filter(_.page.equals(ApplicationHistoryPage))

          actual mustBe empty
        }
      }
    }
  }

}
