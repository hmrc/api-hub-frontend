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
import com.typesafe.config.ConfigFactory
import config.{FrontendAppConfig, HipEnvironments}
import controllers.actions.{FakeApplication, FakeSupporter}
import fakes.FakeHipEnvironments
import models.application.ApplicationLenses.*
import models.user.UserModel
import org.scalatest.Inspectors
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
import play.api.i18n.Messages
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import utils.TestHelpers
import viewmodels.SideNavItem
import viewmodels.SideNavItem.{SideNavItemBranch, SideNavItemLeaf}
import viewmodels.application.ApplicationSideNavPages.*

class ApplicationNavItemsSpec extends SpecBase with Matchers with TestHelpers with TableDrivenPropertyChecks with Inspectors {

  "ApplicationNavItems" - {
    "must return the correct list of nav items" in {
      val config = Configuration(ConfigFactory.parseString(
        """
          |features {
          |  application-details-environments-left-side-nav: true
          |}
          |""".stripMargin
      ))
      val playApplication = applicationBuilder(
        None,
        testConfiguration = config
      ).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)
        implicit val config: FrontendAppConfig = playApplication.injector.instanceOf[FrontendAppConfig]
        implicit val hipEnvironments: HipEnvironments = playApplication.injector.instanceOf[HipEnvironments]
        val applicationNavItems = playApplication.injector.instanceOf[ApplicationNavItems]

        val actual = applicationNavItems(Some(FakeSupporter), FakeApplication, DetailsPage)
        val expected = Seq(
          SideNavItemLeaf(
            DetailsPage,
            "Application details",
            controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id),
            isCurrentPage = true
          ),
          SideNavItemBranch(
            title = "Environments",
            sideNavItems = Seq(
              SideNavItemLeaf(
                page = EnvironmentPage(FakeHipEnvironments.production),
                title = "Production environment",
                link = controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, FakeHipEnvironments.production.id),
                isCurrentPage = false,
              ),
              SideNavItemLeaf(
                page = EnvironmentPage(FakeHipEnvironments.test),
                title = "Test environment",
                link = controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, FakeHipEnvironments.test.id),
                isCurrentPage = false,
              ),
            )
          ),
          SideNavItemLeaf(
            ManageTeamMembersPage,
            "Manage team members",
            controllers.application.routes.ManageTeamMembersController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItemLeaf(
            DeleteApplicationPage,
            "Delete application",
            controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItemLeaf(
            page = ChangeOwningTeamPage,
            title = "Change owning team",
            link = controllers.application.routes.UpdateApplicationTeamController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItemLeaf(
            page = ApplicationHistoryPage,
            title = "Application history",
            link = controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          ),
          SideNavItemLeaf(
            page = ViewAsJsonApplicationPage,
            title = "View as JSON",
            link = controllers.application.routes.ApplicationSupportController.onPageLoad(FakeApplication.id),
            isCurrentPage = false,
            opensInNewTab = true
          ),
          SideNavItemLeaf(
            page = AllScopesPage,
            title = "All scopes",
            link = controllers.application.routes.AllScopesController.onPageLoad(FakeApplication.id),
            isCurrentPage = false
          )
        )

        actual mustBe expected
      }
    }

//    "must select the correct current page" in {
//      val pages = Table(
//        "page",
//        DetailsPage,
//        EnvironmentPage(FakeHipEnvironments.test),
//        EnvironmentPage(FakeHipEnvironments.production),
//        ManageTeamMembersPage,
//        DeleteApplicationPage
//      )
//
//      val config = Configuration(ConfigFactory.parseString(
//        """
//          |hipEnvironments = {
//          |    production = {
//          |        id = "production",
//          |        rank = 1,
//          |        nameKey = "hipEnvironment.production.name",
//          |        environmentName = "primary",
//          |        isProductionLike = true
//          |    },
//          |    test = {
//          |        id = "test",
//          |        rank = 2,
//          |        nameKey = "hipEnvironment.test.name",
//          |        environmentName = "secondary",
//          |        isProductionLike = false
//          |    }
//          |}""".stripMargin
//      ))
//      val playApplication = applicationBuilder(
//        None,
//        testConfiguration = config
//      ).build()
//
//      running(playApplication) {
//        implicit val implicitMessages: Messages = messages(playApplication)
//        implicit val config: FrontendAppConfig = playApplication.injector.instanceOf[FrontendAppConfig]
//        implicit val hipEnvironments: HipEnvironments = playApplication.injector.instanceOf[HipEnvironments]
//        val applicationNavItems = playApplication.injector.instanceOf[ApplicationNavItems]
//
//        forAll(pages) {
//          page =>
//
//            val actual = applicationNavItems(None, FakeApplication, page)
//              .collect { case ni: SideNavItemLeaf => ni }
//              .filter(_.isCurrentPage)
//              .map(_.page)
//
//            actual mustBe Seq(page)
//        }
//      }
//    }
//
//    "must not return the Manage team members item for applications with a global team" in {
//      val playApplication = applicationBuilder(None).build()
//
//      running(playApplication) {
//        implicit val implicitMessages: Messages = messages(playApplication)
//        implicit val config: FrontendAppConfig = playApplication.injector.instanceOf[FrontendAppConfig]
//        implicit val hipEnvironments: HipEnvironments = playApplication.injector.instanceOf[HipEnvironments]
//        val applicationNavItems = playApplication.injector.instanceOf[ApplicationNavItems]
//
//        val actual = applicationNavItems(Some(FakeSupporter), FakeApplication.setTeamId("test-team-id"), DetailsPage)
//          .collect { case ni: SideNavItemLeaf => ni }
//          .filter(_.page.equals(ManageTeamMembersPage))
//
//        actual mustBe empty
//      }
//    }
//
//    "must not display support-only items for non-support users" in {
//      val supportPages = Set(ViewAsJsonApplicationPage, AllScopesPage)
//
//      forAll(usersWhoCannotSupport) { (user: UserModel) =>
//        val playApplication = applicationBuilder(None).build()
//
//        running(playApplication) {
//          implicit val implicitMessages: Messages = messages(playApplication)
//          implicit val config: FrontendAppConfig = playApplication.injector.instanceOf[FrontendAppConfig]
//          implicit val hipEnvironments: HipEnvironments = playApplication.injector.instanceOf[HipEnvironments]
//          val applicationNavItems = playApplication.injector.instanceOf[ApplicationNavItems]
//
//          val actual = applicationNavItems(Some(user), FakeApplication.setTeamId("test-team-id"), DetailsPage)
//            .collect { case ni: SideNavItemLeaf => ni }
//            .filter(_.page.equals(ViewAsJsonApplicationPage))
//
//          forAll (actual) {page => supportPages must not contain page.page}
//        }
//      }
//    }
//
  }

}
