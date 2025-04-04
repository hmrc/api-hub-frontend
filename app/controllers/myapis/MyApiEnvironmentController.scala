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

import cats.implicits.toTraverseOps
import com.google.inject.{Inject, Singleton}
import config.{FrontendAppConfig, HipEnvironment, HipEnvironments}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.requests.ApiRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.MyApiEnvironmentViewModel
import views.html.myapis.MyApiEnvironmentView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyApiEnvironmentController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  view: MyApiEnvironmentView,
  identify: IdentifierAction,
  config: FrontendAppConfig,
  apiAuth: ApiAuthActionProvider,
  errorResultBuilder: ErrorResultBuilder,
  apiHubService: ApiHubService,
  hipEnvironments: HipEnvironments,
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String, environment: String): Action[AnyContent] = (identify andThen apiAuth(id)) async {
    implicit request =>
        hipEnvironments.forEnvironmentIdOptional(environment)
          .map(hipEnvironment =>
            for {
              deploymentStatuses <- apiHubService.getApiDeploymentStatuses(request.apiDetails.publisherReference)
              selectedEgress <- getEgress(hipEnvironment)
              team <- request.apiDetails.teamId.flatTraverse(apiHubService.findTeamById)
              viewModel = MyApiEnvironmentViewModel(
                request.apiDetails,
                hipEnvironment,
                hipEnvironment.promoteTo,
                request.identifierRequest.user,
                deploymentStatuses,
                selectedEgress,
                team
              )
            } yield Ok(view(viewModel))
          ).getOrElse(
            Future.successful(errorResultBuilder.environmentNotFound(environment))
          )
  }

  private def getEgress(hipEnvironment: HipEnvironment)(implicit request: ApiRequest[?]): Future[Option[String]] =
    apiHubService.getDeploymentDetails(request.apiDetails.publisherReference, hipEnvironment)
      .map(_.flatMap(deploymentDetails =>
          Option.when(deploymentDetails.hasEgress)(deploymentDetails.egress).flatten
      ))


  def onSubmit(id: String, environment: String): Action[AnyContent] = (identify andThen apiAuth(id)) {
    implicit request =>
      Redirect(controllers.myapis.promote.routes.MyApiSetEgressController.onPageLoad(id, environment))
  }

}
