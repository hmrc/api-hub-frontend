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

import models.application.Application
import models.application.ApplicationLenses._
import models.user.UserModel
import play.api.i18n.Messages
import viewmodels.{SideNavItem, SideNavPage}

object ApplicationSideNavPages {

  case object DetailsPage extends SideNavPage
  case object ApisPage extends SideNavPage
  case object EnvironmentsAndCredentialsPage extends SideNavPage
  case object ManageTeamMembersPage extends SideNavPage
  case object DeleteApplicationPage extends SideNavPage
  case object ChangeOwningTeamPage extends SideNavPage
  case object ApplicationHistoryPage extends SideNavPage
  case object ViewAsJsonApplicationPage extends SideNavPage
  case object AllScopesPage extends SideNavPage

}

object ApplicationNavItems {

  import ApplicationSideNavPages._

  def apply(userModel: Option[UserModel], application: Application, currentPage: SideNavPage)(implicit messages: Messages): Seq[SideNavItem] = {
    Seq(
      Some(SideNavItem(
        page = DetailsPage,
        title = messages("applicationNav.page.applicationDetails"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == DetailsPage
      )),
      Some(SideNavItem(
        page = ApisPage,
        title = messages("applicationNav.page.applicationApis"),
        link = controllers.application.routes.ApplicationApisController.onPageLoad(application.id),
        isCurrentPage = currentPage == ApisPage
      )),
      Some(SideNavItem(
        page = EnvironmentsAndCredentialsPage,
        title = messages("applicationNav.page.environmentsAndCredentials"),
        link = controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(application.id),
        isCurrentPage = currentPage == EnvironmentsAndCredentialsPage
      )),
      if (!application.isTeamMigrated) {
        Some(SideNavItem(
          page = ManageTeamMembersPage,
          title = messages("applicationNav.page.manageTeamMembers"),
          link = controllers.application.routes.ManageTeamMembersController.onPageLoad(application.id),
          isCurrentPage = currentPage == ManageTeamMembersPage
        ))
      }
      else {
        None
      },
      Some(SideNavItem(
        page = DeleteApplicationPage,
        title = messages("applicationNav.page.deleteApplication"),
        link = controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(application.id),
        isCurrentPage = currentPage == DeleteApplicationPage
      )),
      Some(SideNavItem(
        page = ChangeOwningTeamPage,
        title = messages("application.update.team.title"),
        link = controllers.application.routes.UpdateApplicationTeamController.onPageLoad(application.id),
        isCurrentPage = currentPage == ChangeOwningTeamPage
      )),
      Some(SideNavItem(
        page = ApplicationHistoryPage,
        title = messages("applicationHistory.title"),
        link = controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(application.id),
        isCurrentPage = currentPage == ApplicationHistoryPage
      )),
      if (userModel.exists(_.permissions.canSupport)) {
        Some(SideNavItem(
          page = ViewAsJsonApplicationPage,
          title = messages("applicationNav.page.viewJson"),
          link = controllers.application.routes.ApplicationSupportController.onPageLoad(application.id),
          isCurrentPage = currentPage == ViewAsJsonApplicationPage,
          opensInNewTab = true
        ))
      }
      else {
        None
      },
      if (userModel.exists(_.permissions.canSupport)) {
        Some(SideNavItem(
          page = AllScopesPage,
          title = messages("applicationNav.page.allScopes"),
          link = controllers.application.routes.AllScopesController.onPageLoad(application.id),
          isCurrentPage = currentPage == AllScopesPage
        ))
      }
      else {
        None
      }
    )
    .flatten
  }

}
