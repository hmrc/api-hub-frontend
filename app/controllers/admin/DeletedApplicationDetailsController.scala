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

package controllers.admin

import com.google.inject.Inject
import controllers.actions.{ApplicationAuthActionProvider, AuthorisedSupportAction, IdentifierAction}
import models.accessrequest.AccessRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.DeletedApplicationDetailsView

import scala.concurrent.ExecutionContext
import scala.math.Ordered.orderingToOrdered

class DeletedApplicationDetailsController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  applicationAuth: ApplicationAuthActionProvider,
  deletedView: DeletedApplicationDetailsView,
  apiHubService: ApiHubService
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private implicit val accessRequestOrdering: Ordering[AccessRequest] = (x: AccessRequest, y: AccessRequest) => {
    y.requested compare x.requested
  }

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen isSupport andThen applicationAuth(id, false, true)).async {
    implicit request =>
      apiHubService.getAccessRequests(Some(request.application.id), None).map {
        accessRequests =>
          Ok(deletedView(
            request.application,
            accessRequests.sorted,
            Some(request.identifierRequest.user)
          ))
      }
  }

}