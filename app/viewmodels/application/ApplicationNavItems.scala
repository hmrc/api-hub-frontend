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

case class ApplicationNavItem(title: String, link: Call, isCurrentPage: Boolean)

object ApplicationNavItems {

  import ApplicationPages._

  def apply(application: Application, currentPage: ApplicationPage)(implicit messages: Messages): Seq[ApplicationNavItem] = {
    Seq(
      ApplicationNavItem(
        title = messages("Application details"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == DetailsPage
      ),
      ApplicationNavItem(
        title = messages("Application APIs"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == ApisPage
      ),
      ApplicationNavItem(
        title = messages("Environments and credentials"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == EnvironmentsAndCredentialsPage
      ),
      ApplicationNavItem(
        title = messages("Manage team members"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == ManageTeamMembersPage
      ),
      ApplicationNavItem(
        title = messages("Edit application name"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == EditApplicationNamePage
      ),
      ApplicationNavItem(
        title = messages("Leave application"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == LeaveApplicationPage
      ),
      ApplicationNavItem(
        title = messages("Delete application"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage == DeleteApplicationPage
      )
    )
  }

}
