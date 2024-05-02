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

import models.user.UserModel
import play.api.i18n.Messages
import viewmodels.{SideNavItem, SideNavPage}

object AdminSideNavPages {

  case object AccessRequestsPage extends SideNavPage
  case object ManageTeamsPage extends SideNavPage
  case object ManageApplicationsPage extends SideNavPage

}

object AdminNavItems {

  import AdminSideNavPages._

  def apply(user: UserModel, currentPage: SideNavPage)(implicit messages: Messages): Seq[SideNavItem] = {
    val accessRequestsNavItem = SideNavItem(
      page = AccessRequestsPage,
      title = messages("accessRequests.title"),
      link = controllers.admin.routes.AccessRequestsController.onPageLoad(),
      isCurrentPage = currentPage == AccessRequestsPage
    )

    if (user.permissions.canSupport) {
      Seq(
        SideNavItem(
          page = ManageApplicationsPage,
          title = messages("manageApplications.title"),
          link = controllers.admin.routes.ManageApplicationsController.onPageLoad(),
          isCurrentPage = currentPage == ManageApplicationsPage
        ),
        SideNavItem(
          page = ManageTeamsPage,
          title = messages("manageTeams.title"),
          link = controllers.admin.routes.ManageTeamsController.onPageLoad(),
          isCurrentPage = currentPage == ManageTeamsPage
        ),
        accessRequestsNavItem
      )
    }
    else {
      Seq(accessRequestsNavItem)
    }
  }

}
