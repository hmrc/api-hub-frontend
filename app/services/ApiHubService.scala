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
import connectors.{ApplicationsConnector, IntegrationCatalogueConnector}
import models.AvailableEndpoint
import models.accessrequest.{AccessRequest, AccessRequestRequest, AccessRequestStatus}
import models.api.{ApiDeploymentStatuses, ApiDetail, ApiDetailSummary, PlatformContact}
import models.application.*
import models.deployment.DeploymentDetails
import models.exception.ApplicationsException
import models.requests.{AddApiRequest, AddApiRequestEndpoint}
import models.stats.ApisInProductionStatistic
import models.team.{NewTeam, Team}
import models.user.UserContactDetails
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiHubService @Inject()(
                               applicationsConnector: ApplicationsConnector,
                               integrationCatalogueConnector: IntegrationCatalogueConnector
                             ) extends Logging {

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

  def getApplication(id: String, enrich: Boolean, includeDeleted: Boolean = false)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    applicationsConnector.getApplication(id, enrich, includeDeleted)
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

  def getApiDeploymentStatuses(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[ApiDeploymentStatuses]] = {
    applicationsConnector.getApiDeploymentStatuses(publisherReference)
  }

  def getDeploymentDetails(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[DeploymentDetails]] = {
    applicationsConnector.getDeploymentDetails(publisherReference)
  }

  def getApis(platform: Option[String] = None)(implicit hc: HeaderCarrier): Future[Seq[ApiDetailSummary]] = {
    integrationCatalogueConnector.getApis(platform)
  }

  def addCredential(id: String, environmentName: EnvironmentName)(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Option[Credential]]] = {
    applicationsConnector.addCredential(id, environmentName)
  }

  def deleteCredential(
                        id: String,
                        environmentName: EnvironmentName,
                        clientId: String
                      )(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Option[Unit]]] = {
    applicationsConnector.deleteCredential(id, environmentName, clientId)
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

  def getUserApis(teamMember: TeamMember)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[ApiDetail]] = {
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

}
