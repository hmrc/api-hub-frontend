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

import models.api.{ApiDeploymentStatus, ApiDeploymentStatuses, ApiDetail}
import models.user.UserModel
import play.api.i18n.Messages
import viewmodels.{SideNavItem, SideNavPage}
import SideNavItem.{SideNavItemBranch, SideNavItemLeaf}
import config.{HipEnvironment, HipEnvironments}

import javax.inject.{Inject, Singleton}

object MyApisNavPages {

  case object ProducerApiDetailsPage extends SideNavPage
  case object UpdateApiPage extends SideNavPage
  case object ChangeOwningTeamPage extends SideNavPage
  case object ApiUsagePage extends SideNavPage
  case object ViewApiAsConsumerPage extends SideNavPage
  case class EnvironmentPage(hipEnvironment: HipEnvironment) extends SideNavPage {
    override def toString: String = hipEnvironment.id
  }

}

@Singleton
class MyApisNavItems @Inject()(hipEnvironments: HipEnvironments) {

  import MyApisNavPages._
  
  private def canUpdateApi(apiDetail: ApiDetail, deploymentStatuses: ApiDeploymentStatuses, hipEnvironments: HipEnvironments): Boolean = {
    val isDeployedToTest = deploymentStatuses.statuses.collectFirst {
      case ApiDeploymentStatus.Deployed(environmentId, _) if environmentId == hipEnvironments.deployTo.id => true
    }.getOrElse(false)

    val isHipApi = apiDetail.isSelfServe
    isHipApi && isDeployedToTest && apiDetail.isSelfServe
  }

  def apply(apiDetail: ApiDetail, user: UserModel, currentPage: SideNavPage, apiDeploymentStatuses: ApiDeploymentStatuses)(implicit messages: Messages): Seq[SideNavItem] = {
    Seq(
      Some(
        SideNavItemLeaf(
          page = ProducerApiDetailsPage,
          title = messages("myApis.details.title"),
          link = controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiDetail.id),
          isCurrentPage = currentPage == ProducerApiDetailsPage
        )
      ),
      Option.when(canUpdateApi(apiDetail, apiDeploymentStatuses, hipEnvironments)) {
        SideNavItemLeaf(
          page = UpdateApiPage,
          title = messages("myApis.update.title"),
          link = controllers.myapis.update.routes.UpdateApiStartController.startProduceApi(apiDetail.id),
          isCurrentPage = currentPage == UpdateApiPage
        )
      },
      Some(
        SideNavItemBranch(
          title = messages("applicationNav.page.environments"),
          sideNavItems = hipEnvironments.environments.reverse.map { hipEnvironment =>
            val environmentPage = EnvironmentPage(hipEnvironment)
            SideNavItemLeaf(
              page = environmentPage,
              title = messages(s"site.environment.${hipEnvironment.id}"),
              link = controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, hipEnvironment.id),
              isCurrentPage = currentPage == environmentPage,
            )
          }
        )
      ),
      Option.when(user.permissions.canSupport) {
        SideNavItemLeaf(
          page = ApiUsagePage,
          title = messages("myApis.usage.link"),
          link = controllers.myapis.routes.ApiUsageController.onPageLoad(apiDetail.id),
          isCurrentPage = currentPage == ApiUsagePage
        )
      },
      Some(
        SideNavItemLeaf(
          page = ViewApiAsConsumerPage,
          title = messages("myApis.viewApiAsConsumer.title"),
          link = controllers.routes.ApiDetailsController.onPageLoad(apiDetail.id),
          isCurrentPage = currentPage == ViewApiAsConsumerPage,
          opensInNewTab = true
        )
      )
    ).flatten
  }
}
