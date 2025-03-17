/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import com.google.inject.{Inject, Singleton}
import config.{FrontendAppConfig, HipEnvironment}
import models.api.{ApiDeployment, EgressGateway}
import models.application.ClientScope
import models.deployment.{DeploymentDetails, StatusResponse, SuccessfulDeploymentResponse}
import models.exception.ApimException
import play.api.Logging
import play.api.http.HeaderNames.*
import play.api.http.Status.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ApimConnector @Inject()(
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig,
  frontendAppConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")
  private val clientAuthToken = frontendAppConfig.appAuthToken

  def getDeployments(hipEnvironment: HipEnvironment)(implicit hc: HeaderCarrier): Future[Either[ApimException, Seq[ApiDeployment]]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/get-deployments/${hipEnvironment.id}")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/json")
      .execute[Either[UpstreamErrorResponse, Seq[ApiDeployment]]]
      .map {
        case Right(deployments) => Right(deployments)
        case Left(e) if e.statusCode == BAD_GATEWAY => Left(ApimException.badGateway())
        case Left(e) => Left(ApimException.unexpectedResponse(e.statusCode))
      }
      .recoverWith(errorHandler())
  }

  def getDeployment(hipEnvironment: HipEnvironment, publisherRef: String)(implicit hc: HeaderCarrier): Future[Either[ApimException, SuccessfulDeploymentResponse]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/get-deployment/${hipEnvironment.id}/$publisherRef")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/json")
      .execute[Either[UpstreamErrorResponse, SuccessfulDeploymentResponse]]
      .map {
        case Right(response) => Right(response)
        case Left(e) if e.statusCode == NOT_FOUND => Left(ApimException.serviceNotFound(publisherRef))
        case Left(e) if e.statusCode == BAD_GATEWAY => Left(ApimException.badGateway())
        case Left(e) => Left(ApimException.unexpectedResponse(e.statusCode))
      }
      .recoverWith(errorHandler())
  }

  def getOpenApiSpecification(hipEnvironment: HipEnvironment, publisherRef: String)(implicit hc: HeaderCarrier): Future[Either[ApimException, String]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/get-open-api-specification/${hipEnvironment.id}/$publisherRef")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/yaml")
      .execute[HttpResponse]
      .map(
        response =>
          if (is2xx(response.status)) {
            Right(response.body)
          }
          else if (response.status == NOT_FOUND) {
            Left(ApimException.serviceNotFound(publisherRef))
          }
          else if (response.status == BAD_GATEWAY) {
            Left(ApimException.badGateway())
          }
          else {
            Left(ApimException.unexpectedResponse(response.status))
          }
      )
      .recoverWith(errorHandler())
  }

  def getDeploymentDetails(hipEnvironment: HipEnvironment, publisherRef: String)(implicit hc: HeaderCarrier): Future[Either[ApimException, DeploymentDetails]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/get-deployment-details/${hipEnvironment.id}/$publisherRef")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/json")
      .execute[Either[UpstreamErrorResponse, DeploymentDetails]]
      .map {
        case Right(response) => Right(response)
        case Left(e) if e.statusCode == NOT_FOUND => Left(ApimException.serviceNotFound(publisherRef))
        case Left(e) if e.statusCode == BAD_GATEWAY => Left(ApimException.badGateway())
        case Left(e) => Left(ApimException.unexpectedResponse(e.statusCode))
      }
      .recoverWith(errorHandler())
  }

  def getDeploymentStatus(
    hipEnvironment: HipEnvironment,
    publisherRef: String,
    mergeRequestIid: String,
    version: String
  )(implicit hc: HeaderCarrier): Future[Either[ApimException, StatusResponse]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/get-deployment-status/${hipEnvironment.id}/$publisherRef?mergeRequestIid=$mergeRequestIid&version=$version")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/json")
      .execute[Either[UpstreamErrorResponse, StatusResponse]]
      .map {
        case Right(response) => Right(response)
        case Left(e) if e.statusCode == NOT_FOUND => Left(ApimException.serviceNotFound(publisherRef))
        case Left(e) if e.statusCode == BAD_GATEWAY => Left(ApimException.badGateway())
        case Left(e) => Left(ApimException.unexpectedResponse(e.statusCode))
      }
      .recoverWith(errorHandler())
  }

  def listEgressGateways(hipEnvironment: HipEnvironment)(implicit hc: HeaderCarrier): Future[Either[ApimException, Seq[EgressGateway]]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/list-egress-gateways/${hipEnvironment.id}")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/json")
      .execute[Either[UpstreamErrorResponse, Seq[EgressGateway]]]
      .map {
        case Right(response) => Right(response)
        case Left(e) if e.statusCode == BAD_GATEWAY => Left(ApimException.badGateway())
        case Left(e) => Left(ApimException.unexpectedResponse(e.statusCode))
      }
      .recoverWith(errorHandler())
  }

  def fetchClientScopes(hipEnvironment: HipEnvironment, clientId: String)(implicit hc: HeaderCarrier): Future[Either[ApimException, Seq[ClientScope]]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/support/apim/fetch-client-scopes/${hipEnvironment.id}/$clientId")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .setHeader(ACCEPT -> "application/json")
      .execute[Either[UpstreamErrorResponse, Seq[ClientScope]]]
      .map {
        case Right(response) => Right(response)
        case Left(e) if e.statusCode == NOT_FOUND => Left(ApimException.credentialNotFound(clientId))
        case Left(e) if e.statusCode == BAD_GATEWAY => Left(ApimException.badGateway())
        case Left(e) => Left(ApimException.unexpectedResponse(e.statusCode))
      }
      .recoverWith(errorHandler())
  }

  private def errorHandler[T](): PartialFunction[Throwable, Future[Either[ApimException, T]]] = {
    (t: Throwable) => t match {
      case NonFatal(e) => Future.successful(Left(ApimException.error(e)))
    }
  }

}
