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
import models.accessrequest.{AccessRequest, AccessRequestDecisionRequest, AccessRequestRequest, AccessRequestStatus}
import models.application._
import models.exception.{ApplicationCredentialLimitException, ApplicationsException}
import models.requests.{AddApiRequest, TeamMemberRequest}
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

  def addApi(applicationId: String, addApiRequest: AddApiRequest)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient
      .put(url"$applicationsBaseUrl/api-hub-applications/applications/$applicationId/apis")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(addApiRequest))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
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

  def addCredential(
    id: String,
    environmentName: EnvironmentName
  )(implicit hc:HeaderCarrier): Future[Either[ApplicationsException, Option[Credential]]] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/$environmentName/credentials")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Credential]]
      .flatMap {
        case Right(credential) => Future.successful(Right(Some(credential)))
        case Left(e) if e.statusCode == 404 => Future.successful(Right(None))
        case Left(e) if e.statusCode == 409 => Future.successful(Left(ApplicationCredentialLimitException.forId(id, environmentName)))
        case Left(e) => Future.failed(e)
      }
  }

  def deleteCredential(
    id: String,
    environmentName: EnvironmentName,
    clientId: String
  )(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Option[Unit]]] = {
    httpClient
      .delete(url"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/$environmentName/credentials/$clientId")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(()) => Future.successful(Right(Some(())))
        case Left(e) if e.statusCode == 404 => Future.successful(Right(None))
        case Left(e) if e.statusCode == 409 => Future.successful(Left(ApplicationCredentialLimitException.forId(id, environmentName)))
        case Left(e) => Future.failed(e)
      }
  }

  def createAccessRequest(request: AccessRequestRequest)(implicit hc:HeaderCarrier): Future[Unit] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/access-requests")
      .setHeader((ACCEPT, JSON))
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(request))
      .execute[Unit]

  }

  def getAccessRequests(applicationId: Option[String], status: Option[AccessRequestStatus])(implicit hc:HeaderCarrier): Future[Seq[AccessRequest]] = {
    val url = (applicationId, status) match {
      case(Some(applicationId), Some(status)) =>
        url"$applicationsBaseUrl/api-hub-applications/access-requests?applicationId=$applicationId&status=$status"
      case(Some(applicationId), None) =>
        url"$applicationsBaseUrl/api-hub-applications/access-requests?applicationId=$applicationId"
      case(None, Some(status)) =>
        url"$applicationsBaseUrl/api-hub-applications/access-requests?status=$status"
      case _ =>
        url"$applicationsBaseUrl/api-hub-applications/access-requests"
    }

    httpClient.get(url)
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[AccessRequest]]
  }

  def getAccessRequest(id: String)(implicit hc:HeaderCarrier): Future[Option[AccessRequest]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/access-requests/$id")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, AccessRequest]]
      .flatMap {
        case Right(accessRequest) => Future.successful(Some(accessRequest))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def approveAccessRequest(id: String, decidedBy: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val decisionRequest = AccessRequestDecisionRequest(decidedBy = decidedBy, rejectedReason = None)

    httpClient.put(url"$applicationsBaseUrl/api-hub-applications/access-requests/$id/approve")
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(decisionRequest))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(()) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def rejectAccessRequest(id: String, decidedBy: String, rejectedReason: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val decisionRequest = AccessRequestDecisionRequest(decidedBy = decidedBy, rejectedReason = Some(rejectedReason))

    httpClient.put(url"$applicationsBaseUrl/api-hub-applications/access-requests/$id/reject")
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(decisionRequest))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(()) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def addTeamMember(id: String, email: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val teamMemberRequest = TeamMemberRequest(email)

    httpClient.post(url"$applicationsBaseUrl/api-hub-applications/applications/$id/team-members")
      .setHeader(CONTENT_TYPE -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(teamMemberRequest))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(()) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

}
