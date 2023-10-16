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

sealed trait ApplicationPage

object ApplicationPages {

  case object DetailsPage extends ApplicationPage
  case object ApisPage extends ApplicationPage
  case object EnvironmentsAndCredentialsPage extends ApplicationPage
  case object ManageTeamMembersPage extends ApplicationPage
  case object EditApplicationNamePage extends ApplicationPage
  case object LeaveApplicationPage extends ApplicationPage
  case object DeleteApplicationPage extends ApplicationPage

}

case class ApplicationNavItem(page: ApplicationPage, title: String, link: Call, isCurrentPage: Boolean)

object ApplicationNavItems {

  import ApplicationPages._

  def apply(application: Application, currentPage: ApplicationPage)(implicit messages: Messages): Seq[ApplicationNavItem] = {
    Seq(
      ApplicationNavItem(
        page = DetailsPage,
        title = messages("applicationNav.page.applicationDetails"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == DetailsPage
      ),
      ApplicationNavItem(
        page = ApisPage,
        title = messages("applicationNav.page.applicationApis"),
        link = controllers.application.routes.ApplicationApisController.onPageLoad(application.id),
        isCurrentPage = currentPage == ApisPage
      ),
      ApplicationNavItem(
        page = EnvironmentsAndCredentialsPage,
        title = messages("applicationNav.page.environmentsAndCredentials"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == EnvironmentsAndCredentialsPage
      ),
      ApplicationNavItem(
        page = ManageTeamMembersPage,
        title = messages("applicationNav.page.manageTeamMembers"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == ManageTeamMembersPage
      ),
      ApplicationNavItem(
        page = EditApplicationNamePage,
        title = messages("applicationNav.page.editApplicationName"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == EditApplicationNamePage
      ),
      ApplicationNavItem(
        page = LeaveApplicationPage,
        title = messages("applicationNav.page.leaveApplication"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == LeaveApplicationPage
      ),
      ApplicationNavItem(
        page = DeleteApplicationPage,
        title = messages("applicationNav.page.deleteApplication"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == DeleteApplicationPage
      )
    )
  }

}
