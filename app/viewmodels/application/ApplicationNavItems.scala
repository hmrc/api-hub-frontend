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

import config.{FrontendAppConfig, HipEnvironment, HipEnvironments}
import models.application.Application
import models.application.ApplicationLenses.*
import models.user.UserModel
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.{SideNavItem, SideNavPage}
import SideNavItem.*

import javax.inject.{Inject, Singleton}

object ApplicationSideNavPages {

  case object DetailsPage extends SideNavPage
  case object ApisPage extends SideNavPage
  case object EnvironmentsAndCredentialsPage extends SideNavPage
  case class EnvironmentPage(hipEnvironment: HipEnvironment) extends SideNavPage {
    override def toString: String = hipEnvironment.id
  }
  case object ManageTeamMembersPage extends SideNavPage
  case object DeleteApplicationPage extends SideNavPage
  case object ApplicationHistoryPage extends SideNavPage
  case object ViewAsJsonApplicationPage extends SideNavPage
  case object AllScopesPage extends SideNavPage

}

@Singleton
class ApplicationNavItems @Inject()(config: FrontendAppConfig, hipEnvironments: HipEnvironments) {

  import ApplicationSideNavPages._

  def apply(userModel: Option[UserModel], application: Application, currentPage: Option[SideNavPage])(implicit messages: Messages): Seq[SideNavItem] = {
    Seq(
      Some(SideNavItemLeaf(
        page = DetailsPage,
        title = messages("applicationNav.page.applicationDetails"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = currentPage.contains(DetailsPage)
      )),
      Some(
          SideNavItemBranch(
            title = messages("applicationNav.page.environments"),
            sideNavItems = hipEnvironments.environments.reverse.map { navItemEnvironment =>
              SideNavItemLeaf(
                page = EnvironmentPage(navItemEnvironment),
                title = messages(s"site.environment.${navItemEnvironment.id}"),
                link = controllers.application.routes.EnvironmentsController.onPageLoad(application.id, navItemEnvironment.id),
                isCurrentPage = currentPage.exists {
                  case EnvironmentPage(env) => env.id == navItemEnvironment.id
                  case _ => false
                }
              )
            }
          )
      ),

      if (!application.isTeamMigrated) {
        Some(SideNavItemLeaf(
          page = ManageTeamMembersPage,
          title = messages("applicationNav.page.manageTeamMembers"),
          link = controllers.application.routes.ManageTeamMembersController.onPageLoad(application.id),
          isCurrentPage = currentPage.contains(ManageTeamMembersPage)
        ))
      }
      else {
        None
      },
      Some(SideNavItemLeaf(
        page = DeleteApplicationPage,
        title = messages("applicationNav.page.deleteApplication"),
        link = controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(application.id),
        isCurrentPage = currentPage.contains(DeleteApplicationPage)
      )),
      Some(SideNavItemLeaf(
        page = ApplicationHistoryPage,
        title = messages("applicationHistory.title"),
        link = controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(application.id),
        isCurrentPage = currentPage.contains(ApplicationHistoryPage)
      )),
      if (userModel.exists(_.permissions.canSupport)) {
        Some(SideNavItemLeaf(
          page = ViewAsJsonApplicationPage,
          title = messages("applicationNav.page.viewJson"),
          link = controllers.application.routes.ApplicationSupportController.onPageLoad(application.id),
          isCurrentPage = currentPage.contains(ViewAsJsonApplicationPage),
          opensInNewTab = true
        ))
      }
      else {
        None
      },
      if (userModel.exists(_.permissions.canSupport)) {
        Some(SideNavItemLeaf(
          page = AllScopesPage,
          title = messages("applicationNav.page.allScopes"),
          link = controllers.application.routes.AllScopesController.onPageLoad(application.id),
          isCurrentPage = currentPage.contains(AllScopesPage)
        ))
      }
      else {
        None
      }
    )
    .flatten
  }

}
