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

package controllers.myapis

import config.HipEnvironments
import cats.implicits.toTraverseOps
import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.MyApiDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyApiDetailsController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  view: MyApiDetailsView,
  identify: IdentifierAction,
  apiAuth: ApiAuthActionProvider,
  errorResultBuilder: ErrorResultBuilder,
  apiHubService: ApiHubService,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen apiAuth(id)) async {
    implicit request => for {
      deploymentStatuses <- apiHubService.getApiDeploymentStatuses(request.apiDetails.publisherReference)
        .map(_.sortStatusesWithHipEnvironments(hipEnvironments))
      maybeTeam <- request.apiDetails.teamId.fold(Future.successful(None))(apiHubService.findTeamById)
    } yield
      Ok(view(request.apiDetails, deploymentStatuses, request.identifierRequest.user, maybeTeam))
  }

}
