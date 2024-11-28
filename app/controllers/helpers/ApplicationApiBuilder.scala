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
import models.accessrequest.{AccessRequest, Pending}
import models.api.ApiDetail
import models.application.*
import play.api.mvc.Request
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import viewmodels.application.*

import scala.concurrent.{ExecutionContext, Future}

class ApplicationApiBuilder @Inject()(
  apiHubService: ApiHubService
)(implicit ec: ExecutionContext) extends FrontendHeaderCarrierProvider {

  def build(application: Application)(implicit request: Request[?]): Future[Seq[ApplicationApi]] = {
    apiHubService.getAccessRequests(Some(application.id), None).flatMap (
      accessRequests =>
        fetchApiDetails(application).map(
          apis =>
            build(
              apis = apis,
              pendingAccessRequests = accessRequests.filter(_.status == Pending),
              applicationScopes = ApplicationScopes(apis, accessRequests)
            )
          )
    )
  }

  private def build(
    apis: Seq[(Api, Option[ApiDetail])],
    pendingAccessRequests: Seq[AccessRequest],
    applicationScopes: ApplicationScopes
  ): Seq[ApplicationApi] = {
    apis.map {
      case (api, Some(apiDetail)) =>
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
                    endpointMethod.summary,
                    endpointMethod.description,
                    endpointMethod.scopes,
                    ApplicationEndpointAccess.production(applicationScopes, pendingAccessRequestCount(apiDetail.id, pendingAccessRequests), endpointMethod),
                    ApplicationEndpointAccess.nonProduction(applicationScopes, pendingAccessRequestCount(apiDetail.id, pendingAccessRequests), endpointMethod)
                  )
              )
        }
        ApplicationApi(apiDetail, endpoints, pendingAccessRequestCount(apiDetail.id, pendingAccessRequests))
      case (api, None) =>
        ApplicationApi(api, pendingAccessRequestCount(api.id, pendingAccessRequests))
    }
  }

  private def pendingAccessRequestCount(apiId: String, pendingAccessRequests: Seq[AccessRequest]): Int = {
    pendingAccessRequests.count(_.apiId == apiId)
  }

  private def fetchApiDetails(application: Application)(implicit request: Request[?]): Future[Seq[(Api, Option[ApiDetail])]] = {
    Future
      .sequence(application.apis.map(fetchApiDetail(_)))
  }

  private def fetchApiDetail(api: Api)(implicit request: Request[?]): Future[(Api, Option[ApiDetail])] = {
    apiHubService.getApiDetail(api.id).map(apiDetail => (api, apiDetail))
  }

}
