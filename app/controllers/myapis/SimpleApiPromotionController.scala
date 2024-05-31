/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import connectors.ApplicationsConnector
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.deployment.{InvalidOasResponse, SuccessfulDeploymentsResponse}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiPromotionView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleApiPromotionController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  apiHubService: ApiHubService,
  view: SimpleApiPromotionView,
  successView: DeploymentSuccessView,
  failureView: DeploymentFailureView,
  errorResultBuilder: ErrorResultBuilder,
  applicationsConnector: ApplicationsConnector
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      apiHubService.getApiDetail(id).map {
        case Some(apiDetail) => Ok(view(apiDetail, request.user))
        case None => errorResultBuilder.apiNotFound(id)
      }
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      apiHubService.getApiDetail(id).flatMap {
        case Some(apiDetail) =>
          applicationsConnector.promoteToProduction(apiDetail.publisherReference).map {
            case Some(response: SuccessfulDeploymentsResponse) =>
              Ok(successView(request.user, response))
            case Some(response: InvalidOasResponse) =>
              BadRequest(
                failureView(
                  request.user,
                  response.failure,
                  controllers.myapis.routes.SimpleApiPromotionController.onPageLoad(id).url
                )
              )
            case None =>
              errorResultBuilder.apiNotFoundInApim(apiDetail)
          }
        case None =>
          Future.successful(errorResultBuilder.apiNotFound(id))
      }
  }

}
