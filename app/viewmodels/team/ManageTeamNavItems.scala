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

package viewmodels.team

import models.team.Team
import play.api.i18n.Messages
import viewmodels.{SideNavItem, SideNavPage}
import SideNavItem.SideNavItemLeaf
import config.FrontendAppConfig
import jakarta.inject.Singleton

import javax.inject.Inject

object ManageTeamSideNavPages {

  case object ManageTeamMembersPage extends SideNavPage
  case object EgressesPage extends SideNavPage
  case object ChangeTeamNamePage extends SideNavPage
  case object ViewTeamApplicationsPage extends SideNavPage

}

@Singleton
class ManageTeamNavItems @Inject()(config: FrontendAppConfig) {

  import ManageTeamSideNavPages._

  def apply(team: Team, currentPage: SideNavPage)(implicit messages: Messages): Seq[SideNavItem] = {
    val items = Seq(
      SideNavItemLeaf(
        page = ManageTeamMembersPage,
        title = messages("manageTeamMembers.title"),
        link = controllers.team.routes.ManageTeamController.onPageLoad(team.id),
        isCurrentPage = currentPage == ManageTeamMembersPage
      ),
      SideNavItemLeaf(
        page = ChangeTeamNamePage,
        title = messages("changeTeamName.title"),
        link = controllers.team.routes.ChangeTeamNameController.onPageLoad(team.id),
        isCurrentPage = currentPage == ChangeTeamNamePage
      ),
      SideNavItemLeaf(
        page = ViewTeamApplicationsPage,
        title = messages("viewTeamApplications.title"),
        link = controllers.team.routes.ViewTeamApplicationsController.onPageLoad(team.id),
        isCurrentPage = currentPage == ViewTeamApplicationsPage
      )
    )

    val teamEgressesItem = SideNavItemLeaf(
      page = EgressesPage,
      title = messages("manageTeam.egresses"),
      link = controllers.team.routes.ManageTeamEgressesController.onPageLoad(team.id),
      isCurrentPage = currentPage == EgressesPage
    )

    if (config.showApisOnDashboard) items.patch(1, Seq(teamEgressesItem), 0) else items
  }

}
