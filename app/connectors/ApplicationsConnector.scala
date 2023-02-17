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
import models.application.{Application, NewApplication, NewScope}
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.http.MimeTypes.JSON
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsConnector @Inject()(
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext) {

  private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications")
      .withBody(Json.toJson(newApplication))
      .execute[Application]
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications")
      .setHeader((ACCEPT, JSON))
      .execute[Seq[Application]]
  }

  def getApplication(id:String)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/$id")
      .setHeader((ACCEPT, JSON))
      .execute[Either[UpstreamErrorResponse, Application]]
      .flatMap {
        case Right(application) => Future.successful(Some(application))
        case Left(e) if e.statusCode==404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def requestAdditionalScope(id: String, newScope: NewScope)(implicit hc: HeaderCarrier): Future[Option[NewScope]] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/scopes")
      .withBody(Json.toJson(Seq(newScope)))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(newScope))
        case Left(e) if e.statusCode==404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def pendingScopes()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/pending-scopes")
      .setHeader((ACCEPT, JSON))
      .execute[Seq[Application]]
  }

  def approveProductionScope(appId: String, scopeName: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpClient
      .put(url"$applicationsBaseUrl/api-hub-applications/applications/$appId/environments/prod/scopes/$scopeName")
      .setHeader((CONTENT_TYPE, JSON))
      .withBody("{\"status\":\"APPROVED\"}")
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => {
          Console.println("SUCCESS!")
          Future.successful(Some("APPROVED"))
        }
        case Left(e) if e.statusCode == 404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }
}
