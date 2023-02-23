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

package connectors

import com.google.inject.{Inject, Singleton}
import connectors.ApplicationsConnector.NewScopeStatus
import models.application._
import models.errors.RequestError
import play.api.Logging
import play.api.http.HeaderNames.ACCEPT
import play.api.http.MimeTypes.JSON
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsConnector @Inject()(
    httpClient: HttpClient,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext) extends ConnectorErrorResponseHandling with Logging {

  private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Either[RequestError, Application]] = {
    httpClient.POST[NewApplication, HttpResponse](s"$applicationsBaseUrl/api-hub-applications/applications", newApplication, Seq((ACCEPT, JSON)))
      .flatMap {
        response =>
          response.status match {
            case success if HttpErrorFunctions.is2xx(success) => responseBodyToModel[Application](response).map(Right(_))
            case BAD_REQUEST => badRequest(response).map(Left(_))
            case _ => Future.failed(connectorException(response))
          }
      }
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient.GET[Seq[Application]](s"$applicationsBaseUrl/api-hub-applications/applications")
  }

  def getApplication(id:String)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    httpClient.GET[HttpResponse](s"$applicationsBaseUrl/api-hub-applications/applications/$id", Seq.empty, Seq((ACCEPT, JSON)))
      .flatMap {
        response =>
          response.status match {
            case success if HttpErrorFunctions.is2xx(success) => responseBodyToModel[Application](response).map(Some(_))
            case NOT_FOUND => Future.successful(None)
            case _ => Future.failed(connectorException(response))
          }
      }
  }

  def requestAdditionalScope(id: String, newScope: NewScope)(implicit hc: HeaderCarrier): Future[Either[RequestError, Option[Unit]]] = {
    httpClient.POST[NewScope, HttpResponse](s"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/scopes", newScope, Seq.empty)
      .flatMap {
        response =>
          response.status match {
            case success if HttpErrorFunctions.is2xx(success) => Future.successful(Right(Some(())))
            case NOT_FOUND => Future.successful(Right(None))
            case _ => Future.failed(connectorException(response))
          }
      }
  }

  def pendingScopes()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient.GET[Seq[Application]](
      s"$applicationsBaseUrl/api-hub-applications/applications/pending-scopes",
      Seq.empty,
      Seq((ACCEPT, JSON))
    )
  }

  def approveProductionScope(appId: String, scopeName: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    httpClient.PUT[NewScopeStatus, HttpResponse](
      s"$applicationsBaseUrl/api-hub-applications/applications/$appId/environments/prod/scopes/$scopeName",
      NewScopeStatus(Approved),
      Seq.empty
    )
      .flatMap {
        response =>
          response.status match {
            case success if HttpErrorFunctions.is2xx(success) => Future.successful(true)
            case NOT_FOUND => Future.successful(false)
            case _ => Future.failed(connectorException(response))
          }
      }
  }

}

object ApplicationsConnector {

  case class NewScopeStatus(status: ScopeStatus)

  object NewScopeStatus {

    implicit val formatNewScopeStatus: Format[NewScopeStatus] = Json.format[NewScopeStatus]

  }

}
