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
import controllers.actions.{FakeSupporter, FakeUser}
import fakes.FakeHipEnvironments
import models.api.ApiDeploymentStatuses
import models.api.ApiDeploymentStatus.*
import models.application.{Primary, Secondary}
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.Helpers.running
import viewmodels.SideNavItem
import viewmodels.SideNavItem.SideNavItemLeaf
import viewmodels.myapis.MyApisNavPages.{ApiUsagePage, ChangeOwningTeamPage, ProducerApiDetailsPage, UpdateApiPage, ViewApiAsConsumerPage}

class MyApisNavItemsSpec extends SpecBase with Matchers with TableDrivenPropertyChecks {

  import MyApisNavItemsSpec._

  "class MyApisNavItems" - {

    val statuses = ApiDeploymentStatuses(Seq(
      Deployed(FakeHipEnvironments.production.id, "1"),
      Deployed(FakeHipEnvironments.test.id, "1")
    ))

    "must return the correct list of nav items for a support user" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)
        val actual = MyApisNavItems(apiId, FakeSupporter, ProducerApiDetailsPage, statuses)
        val expected = Seq(producerApiDetailsPage(), updateApiPage(), apiUsagePage(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for a non-support user" in {
      val playApplication = applicationBuilder(None).build()
      
      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)
        val actual = MyApisNavItems(apiId, FakeUser, ProducerApiDetailsPage, statuses)
        val expected = Seq(producerApiDetailsPage(), updateApiPage(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must return the correct list of nav items for a non-deployed api" in {
      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)
        val actual = MyApisNavItems(
          apiId,
          FakeUser,
          ProducerApiDetailsPage,
          ApiDeploymentStatuses(Seq(NotDeployed(FakeHipEnvironments.production.id), NotDeployed(FakeHipEnvironments.test.id)))
        )
        val expected = Seq(producerApiDetailsPage(), viewApiAsConsumerPage())

        actual mustBe expected
      }
    }

    "must select the correct current page" in {
      val pages = Table(
        "Page",
        ProducerApiDetailsPage,
        UpdateApiPage,
        ApiUsagePage
      )

      val playApplication = applicationBuilder(None).build()

      running(playApplication) {
        implicit val implicitMessages: Messages = messages(playApplication)

        forAll(pages) {
          page =>
            val actual = MyApisNavItems(apiId, FakeSupporter, page, statuses)
              .collect { case ni: SideNavItemLeaf => ni }
              .filter(_.isCurrentPage)
              .map(_.page)

            actual mustBe Seq(page)
        }
      }

    }
  }

}

object MyApisNavItemsSpec {
  val apiId = "apiId"

  private def producerApiDetailsPage(): SideNavItem = {
    SideNavItemLeaf(
      page = ProducerApiDetailsPage,
      title = "API details",
      link = controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiId),
      isCurrentPage = true
    )
  }

  private def updateApiPage(): SideNavItem = {
    SideNavItemLeaf(
      page = UpdateApiPage,
      title = "Update API",
      link = controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(apiId),
      isCurrentPage = false
    )
  }

  private def viewApiAsConsumerPage(): SideNavItem = {
    SideNavItemLeaf(
      page = ViewApiAsConsumerPage,
      title = "View API as consumer",
      link = controllers.routes.ApiDetailsController.onPageLoad(apiId),
      isCurrentPage = false,
      opensInNewTab = true
    )
  }

  private def apiUsagePage(): SideNavItem = {
    SideNavItemLeaf(
      page = ApiUsagePage,
      title = "View API usage",
      link = controllers.myapis.routes.ApiUsageController.onPageLoad(apiId),
      isCurrentPage = false
    )
  }

}
