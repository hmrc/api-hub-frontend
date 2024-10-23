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
import models.accessrequest.{AccessRequest, AccessRequestCancelRequest, AccessRequestDecisionRequest, AccessRequestRequest, AccessRequestStatus}
import models.api.{ApiDeploymentStatuses, ApiDetailSummary, EgressGateway}
import models.api.ApiDeploymentStatuses.readApiDeploymentStatuses
import models.application.*
import models.deployment.*
import models.exception.{ApplicationCredentialLimitException, ApplicationsException, TeamNameNotUniqueException}
import models.requests.{AddApiRequest, ChangeTeamNameRequest, TeamMemberRequest}
import models.stats.ApisInProductionStatistic
import models.team.{NewTeam, Team}
import models.user.UserContactDetails
import play.api.Logging
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import play.api.http.MimeTypes.JSON
import play.api.http.Status.*
import play.api.i18n.{Messages, MessagesProvider}
import play.api.libs.json.{JsResultException, JsString, Json}
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsConnector @Inject()(
                                       httpClient: HttpClientV2,
                                       crypto: ApplicationCrypto,
                                       servicesConfig: ServicesConfig,
                                       frontEndConfig: FrontendAppConfig
                                     )(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")
  private val clientAuthToken = frontEndConfig.appAuthToken

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/applications")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(newApplication))
      .execute[Application]
  }

  def getApplications(userEmail:Option[String], includeDeleted: Boolean)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    val applicationsUrl = userEmail match {
      case Some(email) =>
        val emailEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(email)).value
        url"$applicationsBaseUrl/api-hub-applications/applications?teamMember=$emailEncrypted&includeDeleted=$includeDeleted"
      case _ =>
        url"$applicationsBaseUrl/api-hub-applications/applications?includeDeleted=$includeDeleted"
    }

    httpClient
      .get(applicationsUrl)
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Application]]
  }

  def getApplicationsUsingApi(apiId: String, includeDeleted: Boolean)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/using-api/$apiId?includeDeleted=$includeDeleted")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Application]]
  }

  def getApplicationsByTeam(teamId: String, includeDeleted: Boolean = false)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/teams/$teamId/applications?includeDeleted=$includeDeleted")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Application]]
  }

  def getApplication(id:String, enrich: Boolean, includeDeleted: Boolean)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/$id?enrich=$enrich&includeDeleted=$includeDeleted")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Application]]
      .flatMap {
        case Right(application) => Future.successful(Some(application))
        case Left(e) if e.statusCode==404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
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

  def removeApi(applicationId: String, apiId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient
      .delete(url"$applicationsBaseUrl/api-hub-applications/applications/$applicationId/apis/$apiId")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
        case Left(e) if e.statusCode == 404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def updateApplicationTeam(applicationId: String, teamId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient
      .put(url"$applicationsBaseUrl/api-hub-applications/applications/$applicationId/teams/$teamId")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
        case Left(e) if e.statusCode == 404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def removeApplicationTeam(applicationId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient
      .delete(url"$applicationsBaseUrl/api-hub-applications/applications/$applicationId/teams")
      .setHeader(AUTHORIZATION -> clientAuthToken)
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

  def cancelAccessRequest(id: String, cancelledBy: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val cancelRequest = AccessRequestCancelRequest(cancelledBy)

    httpClient.put(url"$applicationsBaseUrl/api-hub-applications/access-requests/$id/cancel")
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(cancelRequest))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(()) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def addTeamMember(id: String, teamMember: TeamMember)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val teamMemberRequest = TeamMemberRequest(teamMember)

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

  def generateDeployment(deploymentsRequest: DeploymentsRequest)(implicit hc: HeaderCarrier): Future[DeploymentsResponse] = {
    httpClient.post(url"$applicationsBaseUrl/api-hub-applications/deployments")
      .setHeader(CONTENT_TYPE -> JSON)
      .setHeader(ACCEPT -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(deploymentsRequest))
      .execute[HttpResponse]
      .flatMap {
        response =>
          if (is2xx(response.status)) {
            handleSuccessfulDeploymentsResponse(response)
          }
          else if (response.status == BAD_REQUEST) {
            handleInvalidOasResponse(response)
          }
          else {
            Future.failed(UpstreamErrorResponse("Unexpected response", response.status))
          }
      }
  }

  def updateDeployment(publisherRef: String,  redeploymentRequest: RedeploymentRequest)(implicit hc: HeaderCarrier): Future[Option[DeploymentsResponse]] = {
    httpClient.put(url"$applicationsBaseUrl/api-hub-applications/deployments/$publisherRef")
      .setHeader(CONTENT_TYPE -> JSON)
      .setHeader(ACCEPT -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(redeploymentRequest))
      .execute[HttpResponse]
      .flatMap {
        response =>
          if (is2xx(response.status)) {
            handleSuccessfulDeploymentsResponse(response).map(Some(_))
          }
          else if (response.status == BAD_REQUEST) {
            handleInvalidOasResponse(response).map(Some(_))
          }
          else if (response.status == NOT_FOUND) {
            Future.successful(None)
          }
          else {
            Future.failed(UpstreamErrorResponse("Unexpected response", response.status))
          }
      }
  }

  def promoteToProduction(publisherRef: String)(implicit hc: HeaderCarrier): Future[Option[DeploymentsResponse]] = {
    httpClient.put(url"$applicationsBaseUrl/api-hub-applications/deployments/$publisherRef/promote")
      .setHeader(ACCEPT -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[HttpResponse]
      .flatMap {
        response =>
          if (is2xx(response.status)) {
            handleSuccessfulDeploymentsResponse(response).map(Some(_))
          }
          else if (response.status == BAD_REQUEST) {
            handleInvalidOasResponse(response).map(Some(_))
          }
          else if (response.status == NOT_FOUND) {
            Future.successful(None)
          }
          else {
            Future.failed(UpstreamErrorResponse("Unexpected response", response.status))
          }
      }
  }

  def getApiDeploymentStatuses(publisherReference: String)(implicit hc:HeaderCarrier): Future[ApiDeploymentStatuses] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/apis/$publisherReference/deployment-status")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, ApiDeploymentStatuses]]
      .flatMap {
        case Right(apiDeploymentStatuses) => Future.successful(apiDeploymentStatuses)
        case Left(e) => Future.failed(e)
      }
  }

  def getDeploymentDetails(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[DeploymentDetails]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/deployments/$publisherReference")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, DeploymentDetails]]
      .flatMap {
        case Right(deploymentDetails) => Future.successful(Some(deploymentDetails))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def findTeamById(id: String)(implicit hc: HeaderCarrier): Future[Option[Team]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/teams/$id")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Team]]
      .flatMap {
        case Right(team) => Future.successful(Some(team))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def findTeamByName(name: String)(implicit hc: HeaderCarrier): Future[Option[Team]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/teams/name/$name")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Team]]
      .flatMap {
        case Right(team) => Future.successful(Some(team))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def findTeams(teamMemberEmail: Option[String])(implicit hc: HeaderCarrier): Future[Seq[Team]] = {
    val url = teamMemberEmail match {
      case Some(emailAddress) =>
        val emailAddressEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(emailAddress)).value
        url"$applicationsBaseUrl/api-hub-applications/teams?teamMember=$emailAddressEncrypted"
      case None =>
        url"$applicationsBaseUrl/api-hub-applications/teams"
    }

    httpClient.get(url)
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[Team]]
  }

  def createTeam(team: NewTeam)(implicit hc:HeaderCarrier): Future[Either[ApplicationsException, Team]] = {
    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/teams")
      .setHeader((ACCEPT, JSON))
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(team))
      .execute[Either[UpstreamErrorResponse, Team]]
      .flatMap {
        case Right(team) => Future.successful(Right(team))
        case Left(e) if e.statusCode == CONFLICT => Future.successful(Left(TeamNameNotUniqueException.forName(team.name)))
        case Left(e) => Future.failed(e)
      }
  }

  def addTeamMemberToTeam(id: String, teamMember: TeamMember)(implicit hc:HeaderCarrier): Future[Option[Unit]] = {
    val request = TeamMemberRequest(teamMember)

    httpClient
      .post(url"$applicationsBaseUrl/api-hub-applications/teams/$id/members")
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(request))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def removeTeamMemberFromTeam(id: String, teamMember: TeamMember)(implicit hc:HeaderCarrier): Future[Option[Unit]] = {
    val encryptedEmail = crypto.QueryParameterCrypto.encrypt(PlainText(teamMember.email)).value

    httpClient
      .delete(url"$applicationsBaseUrl/api-hub-applications/teams/$id/members/$encryptedEmail")
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def changeTeamName(id: String, newName: String)(implicit hc:HeaderCarrier): Future[Either[ApplicationsException,Unit]] = {
    val request = ChangeTeamNameRequest(newName)

    httpClient
      .put(url"$applicationsBaseUrl/api-hub-applications/teams/$id")
      .setHeader((CONTENT_TYPE, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .withBody(Json.toJson(request))
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Right(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(Right(()))
        case Left(e) if e.statusCode == CONFLICT => Future.successful(Left(TeamNameNotUniqueException.forName(newName)))
        case Left(e) => Future.failed(e)
      }
  }

  def getUserContactDetails()(implicit hc: HeaderCarrier): Future[Seq[UserContactDetails]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/users")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[UserContactDetails]]
  }

  def updateApiTeam(apiId: String, teamId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient.put(url"$applicationsBaseUrl/api-hub-applications/apis/$apiId/teams/$teamId")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def removeApiTeam(apiId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    httpClient.delete(url"$applicationsBaseUrl/api-hub-applications/apis/$apiId/teams")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(Some(()))
        case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def apisInProduction()(implicit hc: HeaderCarrier): Future[ApisInProductionStatistic] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/stats/apis-in-production")
      .setHeader(ACCEPT -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[ApisInProductionStatistic]
  }

  def validateOAS(oas: String)
                 (implicit hc: HeaderCarrier, messagesProvider: MessagesProvider): Future[Either[InvalidOasResponse, Unit]] = httpClient.post(url"$applicationsBaseUrl/api-hub-applications/oas/validate")
                   .setHeader(ACCEPT -> JSON)
                   .setHeader(CONTENT_TYPE -> "text/plain")
                   .setHeader(AUTHORIZATION -> clientAuthToken)
                   .withBody(oas)
                   .execute[HttpResponse]
                   .flatMap {
                     response =>
                       if (is2xx(response.status)) Future.successful(Right(()))
                       else if (response.status == BAD_REQUEST) {
                         handleInvalidOasResponse(response).map { failure =>
                           logger.warn(s"Error while validating OAS:\n${Json.prettyPrint(Json.toJson(failure))}")
                           Left(failure)
                         }
                       } else
                         Future.failed(UpstreamErrorResponse("Unexpected response", response.status))
                   }

  def listApisInProduction()(implicit hc: HeaderCarrier): Future[Seq[ApiDetailSummary]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/stats/list-apis-in-production")
      .setHeader(ACCEPT -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[ApiDetailSummary]]
  }

  private def handleSuccessfulDeploymentsResponse(response: HttpResponse) =
    response.json.validate[SuccessfulDeploymentsResponse].fold(
      invalid => Future.failed(JsResultException(invalid)),
      response => Future.successful(response)
    )

  private def handleInvalidOasResponse(response: HttpResponse) =
    (if (response.body.isEmpty) None
    else
      response.json.validate[InvalidOasResponse].fold(
        _ => None,
        response => Some(response)
      ))
    .map(invalidOasResponse => Future.successful(invalidOasResponse))
    .getOrElse(Future.failed(UpstreamErrorResponse("Bad request", response.status)))

  def listEgressGateways()(implicit hc: HeaderCarrier): Future[Seq[EgressGateway]] = {
    httpClient.get(url"$applicationsBaseUrl/api-hub-applications/egresses/gateways")
      .setHeader(ACCEPT -> JSON)
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Seq[EgressGateway]]
  }
}
