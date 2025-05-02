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

package services

import com.google.inject.{Inject, Singleton}
import config.{HipEnvironment, Platforms, ShareableHipConfig}
import connectors.{ApimConnector, ApplicationsConnector, IntegrationCatalogueConnector}
import models.AvailableEndpoint
import models.accessrequest.{AccessRequest, AccessRequestRequest, AccessRequestStatus}
import models.api.{ApiDeploymentStatus, ApiDeploymentStatuses, ApiDetail, ApiDetailSummary, EgressGateway, PlatformContact}
import models.application.*
import models.deployment.{DeploymentDetails, DeploymentsRequest, DeploymentsResponse, RedeploymentRequest}
import models.event.{EntityType, Event}
import models.exception.ApplicationsException
import models.requests.{AddApiRequest, AddApiRequestEndpoint}
import models.stats.{ApisInProductionStatistic, DashboardStatistics, DashboardStatisticsBuilder}
import models.team.{NewTeam, Team}
import models.user.{UserContactDetails, UserModel}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.admin.ApimRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiHubService @Inject()(
  applicationsConnector: ApplicationsConnector,
  integrationCatalogueConnector: IntegrationCatalogueConnector,
  apimConnector: ApimConnector,
  dashboardStatisticsBuilder: DashboardStatisticsBuilder,
  platforms: Platforms
)(implicit ec: ExecutionContext) extends Logging {

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    logger.debug(s"Registering application named '${newApplication.name}', created by user with email '${newApplication.createdBy.email}''")
    applicationsConnector.registerApplication(newApplication)
  }

  def getApplications(userEmail: Option[String], includeDeleted: Boolean)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.getApplications(userEmail, includeDeleted)
  }

  def getApplicationsUsingApi(apiId: String, includeDeleted: Boolean)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.getApplicationsUsingApi(apiId, includeDeleted)
  }

  def getApplicationsByTeamId(teamId: String, includeDeleted: Boolean = false)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.getApplicationsByTeam(teamId, includeDeleted)
  }

  def getApplication(id: String, includeDeleted: Boolean = false)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    applicationsConnector.getApplication(id, includeDeleted)
  }

  def deleteApplication(id: String, userEmail: Option[String])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    logger.debug(s"Deleting application with ID $id")
    applicationsConnector.deleteApplication(id, userEmail)
  }

  def addApi(applicationId: String, apiId: String, apiTitle: String, availableEndpoints: Seq[AvailableEndpoint])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val scopes = availableEndpoints.flatMap(ae => ae.endpointMethod.scopes)
    val endpoints = availableEndpoints.map(ae => AddApiRequestEndpoint(ae.endpointMethod.httpMethod, ae.path))
    applicationsConnector.addApi(applicationId, AddApiRequest(apiId, apiTitle, endpoints, scopes))
  }

  def removeApi(applicationId: String, apiId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.removeApi(applicationId, apiId)
  }

  def testConnectivity()(implicit hc: HeaderCarrier): Future[String] = {
    applicationsConnector.testConnectivity()
  }

  def getApiDetail(id: String)(implicit hc: HeaderCarrier): Future[Option[ApiDetail]] = {
    integrationCatalogueConnector.getApiDetail(id)
  }

  def getApiDetailForPublishReference(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[ApiDetail]] = {
    integrationCatalogueConnector.getApiDetailForPublishReference(publisherReference)
  }

  def getApiDeploymentStatuses(publisherReference: String)(implicit hc: HeaderCarrier): Future[ApiDeploymentStatuses] = {
    applicationsConnector.getApiDeploymentStatuses(publisherReference)
  }

  def getApiDeploymentStatus(hipEnvironment: HipEnvironment, publisherReference: String)(implicit hc: HeaderCarrier): Future[ApiDeploymentStatus] = {
    applicationsConnector.getApiDeploymentStatus(hipEnvironment, publisherReference)
  }

  def getDeploymentDetails(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[DeploymentDetails]] = {
    applicationsConnector.getDeploymentDetails(publisherReference)
  }

  def getDeploymentDetails(publisherReference: String, hipEnvironment: HipEnvironment)(implicit hc: HeaderCarrier): Future[Option[DeploymentDetails]] = {
    applicationsConnector.getDeploymentDetails(publisherReference, Some(hipEnvironment))
  }

  def generateDeployment(deploymentsRequest: DeploymentsRequest)(implicit hc: HeaderCarrier): Future[DeploymentsResponse] =
    applicationsConnector.generateDeployment(deploymentsRequest)

  def getApis(platform: Option[String] = None)(implicit hc: HeaderCarrier): Future[Seq[ApiDetailSummary]] = {
    integrationCatalogueConnector.getApis(platform)
      .map(_.map(api => api.copy(
        isEISManaged = Some(platforms.isEISManaged(api.platform)),
        isSelfServe = Some(platforms.isSelfServe(api.platform)),
      )))
  }

  def addCredential(id: String, hipEnvironment: HipEnvironment)(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Option[Credential]]] = {
    applicationsConnector.addCredential(id, hipEnvironment)
  }

  def deleteCredential(
                        id: String,
                        hipEnvironment: HipEnvironment,
                        clientId: String
                      )(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Option[Unit]]] = {
    applicationsConnector.deleteCredential(id, hipEnvironment, clientId)
  }

  def getAccessRequests(applicationId: Option[String], status: Option[AccessRequestStatus])(implicit hc: HeaderCarrier): Future[Seq[AccessRequest]] = {
    applicationsConnector.getAccessRequests(applicationId, status)
  }

  def getAccessRequest(id: String)(implicit hc: HeaderCarrier): Future[Option[AccessRequest]] = {
    applicationsConnector.getAccessRequest(id)
  }

  def approveAccessRequest(id: String, decidedBy: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.approveAccessRequest(id, decidedBy)
  }

  def promoteAPI(
                  publisherRef: String,
                  deploymentFrom: HipEnvironment,
                  deploymentTo: HipEnvironment,
                  egress: String
                )(implicit hc: HeaderCarrier): Future[Option[DeploymentsResponse]] =
    applicationsConnector.promoteAPI(
      publisherRef,
      deploymentFrom,
      deploymentTo,
      egress,
    )

  def cancelAccessRequest(id: String, cancelledBy: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.cancelAccessRequest(id, cancelledBy)
  }

  def rejectAccessRequest(id: String, decidedBy: String, rejectedReason: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.rejectAccessRequest(id, decidedBy, rejectedReason)
  }

  def requestProductionAccess(accessRequest: AccessRequestRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    applicationsConnector.createAccessRequest(accessRequest)
  }

  def addTeamMember(id: String, teamMember: TeamMember)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    logger.debug(s"Adding team member to application $id")
    applicationsConnector.addTeamMember(id, teamMember)
  }

  def findTeamById(id: String)(implicit hc: HeaderCarrier): Future[Option[Team]] = {
    logger.debug(s"Fetching team $id")
    applicationsConnector.findTeamById(id)
  }

  def findTeamByName(name: String)(implicit hc: HeaderCarrier): Future[Option[Team]] = {
    logger.debug(s"Fetching team $name")
    applicationsConnector.findTeamByName(name)
  }

  def findTeams(teamMemberEmail: Option[String])(implicit hc: HeaderCarrier): Future[Seq[Team]] = {
    applicationsConnector.findTeams(teamMemberEmail)
  }

  def createTeam(team: NewTeam)(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Team]] = {
    applicationsConnector.createTeam(team)
  }

  def addTeamMemberToTeam(id: String, teamMember: TeamMember)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    logger.debug(s"Adding team member to team $id")
    applicationsConnector.addTeamMemberToTeam(id, teamMember)
  }

  def removeTeamMemberFromTeam(id: String, teamMember: TeamMember)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    logger.debug(s"Removing team member from team $id")
    applicationsConnector.removeTeamMemberFromTeam(id, teamMember)
  }

  def getUserApis(user: UserModel)(implicit hc: HeaderCarrier): Future[Seq[ApiDetail]] = {
    getUserApis(TeamMember(user.email))
  }

  def getUserApis(teamMember: TeamMember)(implicit hc: HeaderCarrier): Future[Seq[ApiDetail]] = {
    findTeams(Some(teamMember.email)) flatMap {
      case teams if teams.nonEmpty => integrationCatalogueConnector.filterApis(teams.map(_.id))
      case _ => Future.successful(Seq.empty)
    }
  }

  def deepSearchApis(searchText: String)(implicit hc: HeaderCarrier): Future[Seq[ApiDetailSummary]]  = {
    integrationCatalogueConnector.deepSearchApis(searchText)
  }

  def changeTeamName(id: String, newName: String)(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Unit]] = {
    logger.debug(s"Changing team name for team $id to $newName")
    applicationsConnector.changeTeamName(id, newName)
  }

  def getUserContactDetails()(implicit hc: HeaderCarrier): Future[Seq[UserContactDetails]] = {
    applicationsConnector.getUserContactDetails()
  }

  def updateDeployment(publisherRef: String,  redeploymentRequest: RedeploymentRequest)(implicit hc: HeaderCarrier): Future[Option[DeploymentsResponse]] =
    applicationsConnector.updateDeployment(publisherRef, redeploymentRequest)

  def updateApiTeam(apiId: String, maybeTeamId: Option[String])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    maybeTeamId match
      case Some(teamId) => applicationsConnector.updateApiTeam(apiId, teamId)
      case None => applicationsConnector.removeApiTeam(apiId)
  }

  def updateApplicationTeam(applicationId: String, maybeTeamId: Option[String])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    maybeTeamId match
      case Some(team) => applicationsConnector.updateApplicationTeam(applicationId, team)
      case None => applicationsConnector.removeApplicationTeam(applicationId)
  }

  def getPlatformContact(forPlatform: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PlatformContact]] = {
    integrationCatalogueConnector.getPlatformContacts() flatMap {
      platformContacts => Future.successful(platformContacts.find(_.platformType == forPlatform))
    }
  }

  def apisInProduction()(implicit hc: HeaderCarrier): Future[ApisInProductionStatistic] = {
    applicationsConnector.apisInProduction()
  }

  def listApisInProduction()(implicit hc: HeaderCarrier): Future[Seq[ApiDetailSummary]] = {
    applicationsConnector.listApisInProduction()
  }

  def listEgressGateways(hipEnvironment: HipEnvironment)(implicit hc: HeaderCarrier): Future[Seq[EgressGateway]] = {
    applicationsConnector.listEgressGateways(hipEnvironment)
  }

  def fetchAllScopes(applicationId: String)(implicit hc: HeaderCarrier): Future[Option[Seq[CredentialScopes]]] = {
    applicationsConnector.fetchAllScopes(applicationId)
  }

  def fetchCredentials(applicationId: String, hipEnvironment: HipEnvironment)(implicit hc: HeaderCarrier): Future[Option[Seq[Credential]]] = {
    applicationsConnector.fetchCredentials(applicationId, hipEnvironment)
  }

  def fixScopes(applicationId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.fixScopes(applicationId)
  }

  def forcePublish(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.forcePublish(publisherReference)
  }
  
  def listEnvironments()(implicit hc: HeaderCarrier): Future[ShareableHipConfig] = {
    applicationsConnector.listEnvironments()
  }

  def testApimEndpoint[T](environment: HipEnvironment, apimRequest: ApimRequest[T], params: Seq[String])(implicit hc: HeaderCarrier): Future[Either[IllegalArgumentException,String]] = {
    apimRequest.makeRequest(apimConnector, environment, params)
  }

  def fetchDashboardStatistics()(implicit hc: HeaderCarrier): Future[DashboardStatistics] = {
    integrationCatalogueConnector.getReport().map(dashboardStatisticsBuilder.build)
  }

  def addEgressesToTeam(teamId: String, egresses: Set[String]) (implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.addEgressesToTeam(teamId, egresses)
  }

  def removeEgressFromTeam(teamId: String, egressId: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.removeEgressFromTeam(teamId, egressId)
  }

  def getEventById(eventId: String)(implicit hc:HeaderCarrier): Future[Option[Event]] = {
    applicationsConnector.findEventById(eventId)
  }
  
  def getEventsByUser(userEmail: String)(implicit hc:HeaderCarrier): Future[Seq[Event]] = {
    applicationsConnector.findEventsByUser(userEmail)
  }

  def getEventsByEntity(entityType: EntityType, entityId: String)(implicit hc: HeaderCarrier): Future[Seq[Event]] = {
    applicationsConnector.findEventsByEntity(entityType, entityId)
  }
}
