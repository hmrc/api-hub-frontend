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

package viewmodels.myapis

import base.SpecBase
import config.HipEnvironments
import controllers.actions.{FakeApiDetail, FakeSupporter, FakeUser}
import fakes.FakeHipEnvironments
import models.api.ApiDeploymentStatuses
import models.api.ApiDeploymentStatus.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Application as PlayApplication
import play.api.i18n.Messages
import play.api.test.Helpers.running
import viewmodels.SideNavItem
import viewmodels.SideNavItem.{SideNavItemBranch, SideNavItemLeaf}
import viewmodels.myapis.MyApisNavPages.{ApiUsagePage, ChangeOwningTeamPage, EnvironmentPage, ProducerApiDetailsPage, UpdateApiPage, ViewApiAsConsumerPage}
import play.api.inject.bind

class MyApisNavItemsSpec extends SpecBase with Matchers with TableDrivenPropertyChecks {

  import MyApisNavItemsSpec._

  "class MyApisNavItems" - {

    val statuses = ApiDeploymentStatuses(Seq(
      Deployed(FakeHipEnvironments.production.id, "1"),
      Deployed(FakeHipEnvironments.test.id, "1")
    ))

    "must return the correct list of nav items for a support user" in {
      val fixture = buildFixture

      running(fixture.application) {
        implicit val implicitMessages: Messages = messages(fixture.application)
        val actual = fixture.myApisNavItems(apiDetail, FakeSupporter, ProducerApiDetailsPage, statuses)
        val expected = Seq(producerApiDetailsPage(), updateApiPage(), environmentsList(), apiUsagePage(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for a non-support user" in {
      val fixture = buildFixture

      running(fixture.application) {
        implicit val implicitMessages: Messages = messages(fixture.application)
        val actual = fixture.myApisNavItems(apiDetail, FakeUser, ProducerApiDetailsPage, statuses)
        val expected = Seq(producerApiDetailsPage(), updateApiPage(), environmentsList(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for a non-deployed api" in {
      val fixture = buildFixture

      running(fixture.application) {
        implicit val implicitMessages: Messages = messages(fixture.application)
        val actual = fixture.myApisNavItems(
          apiDetail,
          FakeUser,
          ProducerApiDetailsPage,
          ApiDeploymentStatuses(Seq(NotDeployed(FakeHipEnvironments.production.id), NotDeployed(FakeHipEnvironments.test.id)))
        )
        val expected = Seq(producerApiDetailsPage(), environmentsList(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for an api not deployed to test" in {
      val fixture = buildFixture

      running(fixture.application) {
        implicit val implicitMessages: Messages = messages(fixture.application)
        val actual = fixture.myApisNavItems(
          apiDetail,
          FakeUser,
          ProducerApiDetailsPage,
          ApiDeploymentStatuses(Seq(Deployed(FakeHipEnvironments.production.id, "1"), NotDeployed(FakeHipEnvironments.test.id)))
        )
        val expected = Seq(producerApiDetailsPage(), environmentsList(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for a non-hip api" in {
      val fixture = buildFixture

      running(fixture.application) {
        implicit val implicitMessages: Messages = messages(fixture.application)
        val actual = fixture.myApisNavItems(apiDetail.copy(platform = "not hip"), FakeSupporter, ProducerApiDetailsPage, statuses)
        val expected = Seq(producerApiDetailsPage(), environmentsList(), apiUsagePage(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must select the correct current page" in {
      val pages = Table(
        "Page",
        ProducerApiDetailsPage,
        UpdateApiPage,
        ApiUsagePage,
        EnvironmentPage(FakeHipEnvironments.production),
        EnvironmentPage(FakeHipEnvironments.test),
      )

      val fixture = buildFixture

      running(fixture.application) {
        implicit val implicitMessages: Messages = messages(fixture.application)
        forAll(pages) {
          page =>
            val actual = fixture.myApisNavItems(apiDetail, FakeSupporter, page, statuses)
              .flatMap {
                case item: SideNavItemLeaf => Seq(item)
                case item: SideNavItemBranch => item.sideNavItems
              }
              .filter(_.isCurrentPage)
              .map(_.page)

            actual mustBe Seq(page)
        }
      }

    }
  }

  private case class Fixture(application: PlayApplication, myApisNavItems: MyApisNavItems)

  private def buildFixture: Fixture = {
    val application = applicationBuilder(None).build()
    val applicationNavItems = application.injector.instanceOf[MyApisNavItems]
    Fixture(application, applicationNavItems)
  }
}

object MyApisNavItemsSpec {
  val apiDetail = FakeApiDetail

  private def producerApiDetailsPage(): SideNavItem = {
    SideNavItemLeaf(
      page = ProducerApiDetailsPage,
      title = "API details",
      link = controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiDetail.id),
      isCurrentPage = true
    )
  }

  private def updateApiPage(): SideNavItem = {
    SideNavItemLeaf(
      page = UpdateApiPage,
      title = "Update API",
      link = controllers.myapis.update.routes.UpdateApiStartController.startProduceApi(apiDetail.id),
      isCurrentPage = false
    )
  }

  private def environmentsList(): SideNavItem = {
    SideNavItemBranch(
      title = "Environments",
      sideNavItems = Seq(
        SideNavItemLeaf(
          page = EnvironmentPage(FakeHipEnvironments.test),
          title = "Test",
          link = controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, "test"),
          isCurrentPage = false
        ),
        SideNavItemLeaf(
          page = EnvironmentPage(FakeHipEnvironments.preProduction),
          title = "Pre-Production",
          link = controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, "preprod"),
          isCurrentPage = false,
        ),
        SideNavItemLeaf(
          page = EnvironmentPage(FakeHipEnvironments.production),
          title = "Production",
          link = controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, "production"),
          isCurrentPage = false,
        ),
      )
    )
  }

  private def viewApiAsConsumerPage(): SideNavItem = {
    SideNavItemLeaf(
      page = ViewApiAsConsumerPage,
      title = "View API as consumer",
      link = controllers.apis.routes.ApiDetailsController.onPageLoad(apiDetail.id),
      isCurrentPage = false,
      opensInNewTab = true
    )
  }

  private def apiUsagePage(): SideNavItem = {
    SideNavItemLeaf(
      page = ApiUsagePage,
      title = "View API usage",
      link = controllers.myapis.routes.ApiUsageController.onPageLoad(apiDetail.id),
      isCurrentPage = false
    )
  }

}
