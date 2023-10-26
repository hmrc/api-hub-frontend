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

package controllers.helpers

import com.google.inject.Inject
import models.api.ApiDetail
import models.application._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Request, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import viewmodels.application._

import scala.concurrent.{ExecutionContext, Future}

class ApplicationApiBuilder @Inject()(
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  override val messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends FrontendHeaderCarrierProvider with I18nSupport {

  def build(application: Application)(implicit request: Request[_]): Future[Either[Result, Seq[ApplicationApi]]] = {
    fetchApiDetails(application).map {
      case Right(apiDetails) => Right(build(application, apiDetails))
      case Left(result) => Left(result)
    }
  }

  private def build(application: Application, apiDetails: Seq[ApiDetail]): Seq[ApplicationApi] = {
    application.apis.flatMap(
      api =>
        apiDetails.find(_.id == api.id).map {
          apiDetail =>
            val endpoints = api.endpoints.flatMap {
              endpoint =>
                apiDetail.endpoints
                  .find(_.path == endpoint.path)
                  .flatMap(_.methods.find(_.httpMethod == endpoint.httpMethod))
                  .map(
                    endpointMethod =>
                      ApplicationEndpoint(
                        endpoint.httpMethod,
                        endpoint.path,
                        endpointMethod.scopes,
                        ApplicationEndpointAccess(application, endpointMethod, Primary),
                        ApplicationEndpointAccess(application, endpointMethod, Secondary)
                      )
                  )
            }
            ApplicationApi(apiDetail, endpoints)
        }
    )
  }

  private def fetchApiDetails(application: Application)(implicit request: Request[_]): Future[Either[Result, Seq[ApiDetail]]] = {
    Future
      .sequence(application.apis.map(fetchApiDetail(_)))
      .map(
        apiDetails =>
          apiDetails.foldLeft[Either[Result, Seq[ApiDetail]]](Right(Seq.empty))(
            (results, apiDetail) =>
              (results, apiDetail) match {
                case (Left(result), _) => Left(result)
                case (_, Left(result)) => Left(result)
                case (Right(apiDetails), Right(apiDetail)) => Right(apiDetails :+ apiDetail)
              }
          )
      )
  }

  private def fetchApiDetail(api: Api)(implicit request: Request[_]): Future[Either[Result, ApiDetail]] = {
    apiHubService.getApiDetail(api.id).map(
      _.toRight(
        errorResultBuilder.notFound(
          Messages("site.apiNotFound.heading"),
          Messages("site.apiNotFound.message", api.id)
        )
      )
    )
  }

}
