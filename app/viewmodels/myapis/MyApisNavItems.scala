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

import models.user.UserModel
import play.api.i18n.Messages
import viewmodels.{SideNavItem, SideNavPage}

object MyApisNavPages {

  case object ProducerApiDetailsPage extends SideNavPage
  case object UpdateApiPage extends SideNavPage
  case object ChangeOwningTeamPage extends SideNavPage
  case object ApiUsagePage extends SideNavPage
}

object MyApisNavItems {

  import MyApisNavPages._

  def apply(apiId: String, user: UserModel, currentPage: SideNavPage)(implicit messages: Messages): Seq[SideNavItem] = {
    Seq(
      SideNavItem(
        page = ProducerApiDetailsPage,
        title = messages("myApis.details.title"),
        link = controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiId),
        isCurrentPage = currentPage == ProducerApiDetailsPage
      ),
      SideNavItem(
        page = UpdateApiPage,
        title = messages("myApis.update.title"),
        link = controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(apiId),
        isCurrentPage = currentPage == UpdateApiPage
      ),
      SideNavItem(
        page = ChangeOwningTeamPage,
        title = messages("myApis.update.team.title"),
        link = controllers.myapis.routes.UpdateApiTeamController.onPageLoad(apiId),
        isCurrentPage = currentPage == ChangeOwningTeamPage
      )
    ) ++ (
      if (user.permissions.canSupport) {
        Some(SideNavItem(
          page = ApiUsagePage,
          title = messages("myApis.usage.link"),
          link = controllers.myapis.routes.ApiUsageController.onPageLoad(apiId),
          isCurrentPage = currentPage == ApiUsagePage
        ))
      } else {
        None
      }
    )
  }
}
