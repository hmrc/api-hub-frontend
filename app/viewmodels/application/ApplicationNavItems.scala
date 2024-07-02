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
import play.api.i18n.Messages
import play.api.mvc.Call
import viewmodels.{SideNavItem, SideNavPage}

object ApplicationSideNavPages {

  case object DetailsPage extends SideNavPage
  case object ApisPage extends SideNavPage
  case object EnvironmentsAndCredentialsPage extends SideNavPage
  case object ManageTeamMembersPage extends SideNavPage
  case object EditApplicationNamePage extends SideNavPage
  case object LeaveApplicationPage extends SideNavPage
  case object DeleteApplicationPage extends SideNavPage

}

object ApplicationNavItems {

  import ApplicationSideNavPages._

  def apply(application: Application, currentPage: SideNavPage)(implicit messages: Messages): Seq[SideNavItem] = {
    Seq(
      SideNavItem(
        page = DetailsPage,
        title = messages("applicationNav.page.applicationDetails"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == DetailsPage
      ),
      SideNavItem(
        page = ApisPage,
        title = messages("applicationNav.page.applicationApis"),
        link = controllers.application.routes.ApplicationApisController.onPageLoad(application.id),
        isCurrentPage = currentPage == ApisPage
      ),
      SideNavItem(
        page = EnvironmentsAndCredentialsPage,
        title = messages("applicationNav.page.environmentsAndCredentials"),
        link = controllers.application.routes.EnvironmentAndCredentialsController.onPageLoad(application.id),
        isCurrentPage = currentPage == EnvironmentsAndCredentialsPage
      ),
      SideNavItem(
        page = ManageTeamMembersPage,
        title = messages("applicationNav.page.manageTeamMembers"),
        link = manageTeamMembersLink(application),
        isCurrentPage = currentPage == ManageTeamMembersPage
      ),
      SideNavItem(
        page = DeleteApplicationPage,
        title = messages("applicationNav.page.deleteApplication"),
        link = controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(application.id),
        isCurrentPage = currentPage == DeleteApplicationPage
      )
    )
  }

  private def manageTeamMembersLink(application: Application): Call = {
    application.teamId match {
      case Some(teamId) => controllers.team.routes.ManageTeamController.onPageLoad(teamId, Some(application.id))
      case None => controllers.application.routes.ManageTeamMembersController.onPageLoad(application.id)
    }
  }

}
