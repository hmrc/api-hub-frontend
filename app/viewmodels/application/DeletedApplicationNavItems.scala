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
import viewmodels.{SideNavItem, SideNavPage}

object DeletedApplicationSideNavPages {
  case object DetailsPage extends SideNavPage
  case object JsonViewPage extends SideNavPage
}

object DeletedApplicationNavItems {
  import DeletedApplicationSideNavPages._

  def apply(application: Application)(implicit messages: Messages): Seq[SideNavItem] = {
    Seq(
      SideNavItem(
        page = DetailsPage,
        title = messages("deletedApplicationNav.page.applicationDetails"),
        link = controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id),
        isCurrentPage = true
      ),
      SideNavItem(
        page = JsonViewPage,
        title = messages("deletedApplicationNav.page.viewJson"),
        link = controllers.application.routes.ApplicationSupportController.onPageLoad(application.id),
        isCurrentPage = false,
        opensInNewTab = true
      ),
    )
  }

}
