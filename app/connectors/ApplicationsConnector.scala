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
import config.FrontendAppConfig
import models.UserEmail
import models.application.{Application, NewApplication, NewScope, Secret}
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import play.api.http.MimeTypes.JSON
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsConnector @Inject()(
    httpClient: HttpClientV2,
    crypto: ApplicationCrypto,
    servicesConfig: ServicesConfig,
    frontEndConfig: FrontendAppConfig
  )(implicit ec: ExecutionContext) {

  private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")
  private val clientAuthToken = frontEndConfig.appAuthToken

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(newApplication))
      .execute[Application]
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Application]]
  }

  def getApplication(id:String, enrich: Boolean)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/$id?enrich=$enrich")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Application]]
      .flatMap {
        case Right(application) => Future.successful(Some(application))
        case Left(e) if e.statusCode==404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def getUserApplications(userEmail:String, enrich: Boolean)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    val emailEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(userEmail)).value
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/?teamMember=$emailEncrypted&enrich=$enrich")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Application]]
  }



  def deleteApplication(id: String, currentUser: Option[String])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications/$id/delete")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(UserEmail(currentUser)))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(()) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def requestAdditionalScope(id: String, newScope: NewScope)(implicit hc: HeaderCarrier): Future[Option[NewScope]] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/scopes")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(Seq(newScope)))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(newScope))
        case Left(e) if e.statusCode==404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def pendingPrimaryScopes()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/pending-primary-scopes")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Application]]
  }

  def approvePrimaryScope(appId: String, scopeName: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    httpClient
      .put(url"$applicationsBaseUrl/api-hub-applications/applications/$appId/environments/primary/scopes/$scopeName")
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody("{\"status\":\"APPROVED\"}")
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(true)
        case Left(e) if e.statusCode == 404 => Future.successful(false)
        case Left(e) => Future.failed(e)
      }
  }

  def createPrimarySecret(id: String)(implicit hc: HeaderCarrier): Future[Option[Secret]] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/primary/credentials/secret")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Secret]]
      .flatMap {
        case Right(secret) => Future.successful(Some(secret))
        case Left(e) if e.statusCode == 404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def testConnectivity()(implicit hc:HeaderCarrier): Future[String] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/test-connectivity")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[HttpResponse]
      .map(response => {
        if (is2xx(response.status)) {
          response.body
        } else {
          s"Response status was ${response.status}"
        }
      })
  }

}
