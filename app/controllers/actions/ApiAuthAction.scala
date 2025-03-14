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

package controllers.actions

import com.google.inject.{Inject, Singleton}
import controllers.helpers.ErrorResultBuilder
import controllers.routes
import models.requests.{ApiRequest, BaseRequest, IdentifierRequest}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.*
import play.api.mvc.{ActionRefiner, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait ApiAuthAction extends ActionRefiner[IdentifierRequest, ApiRequest]

trait ApiAuthActionProvider {
  def apply(apiId: String)(implicit ec: ExecutionContext): ApiAuthAction
}

@Singleton
class ApiAuthActionProviderImpl @Inject()(
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  override val messagesApi: MessagesApi
) extends ApiAuthActionProvider with I18nSupport {

  def apply(apiId: String)(implicit ec: ExecutionContext): ApiAuthAction = {
    new ApiAuthAction with FrontendHeaderCarrierProvider {
      override protected def refine[A](identifierRequest: IdentifierRequest[A]): Future[Either[Result, ApiRequest[A]]] = {
        implicit val request: BaseRequest[?] = identifierRequest

        apiHubService.getApiDetail(apiId).flatMap {
          case Some(apiDetail) if identifierRequest.user.permissions.canSupport =>
            Future.successful(Right(ApiRequest(identifierRequest, apiDetail)))

          case Some(apiDetail) if apiDetail.teamId.isEmpty =>
            Future.successful(Left(Redirect(routes.UnauthorisedController.onPageLoad)))

          case Some(apiDetail) =>
            apiHubService.findTeams(Some(identifierRequest.user.email)).map(
              userTeams =>
                if (userTeams.exists(team => apiDetail.teamId.contains(team.id))) {
                  Right(ApiRequest(identifierRequest, apiDetail))
                } else {
                  Left(Redirect(routes.UnauthorisedController.onPageLoad))
                }
            )

          case None =>
            Future.successful(Left(
              errorResultBuilder.notFound(
                Messages("site.apiNotFound.heading"),
                Messages("site.apiNotFound.message", apiId)
              )
            ))
        }
      }

      override protected def executionContext: ExecutionContext = ec
    }
  }
}
