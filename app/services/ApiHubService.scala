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
import models.api.{ApiDeploymentStatuses, ApiDetail}
import models.application.{Application, Credential, EnvironmentName, NewApplication, TeamMember}
import models.exception.ApplicationsException
import models.requests.{AddApiRequest, AddApiRequestEndpoint}
import models.team.NewTeam
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ApiHubService @Inject()(
  applicationsConnector: ApplicationsConnector,
  integrationCatalogueConnector: IntegrationCatalogueConnector
) extends Logging {

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    logger.debug(s"Registering application named '${newApplication.name}', created by user with email '${newApplication.createdBy.email}''")
    applicationsConnector.registerApplication(newApplication)
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.getApplications()
  }

  def getApplication(id:String, enrich: Boolean)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    applicationsConnector.getApplication(id, enrich)
  }

  def deleteApplication(id: String, userEmail: Option[String])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    logger.debug(s"Deleting application with Id $id")
    applicationsConnector.deleteApplication(id, userEmail)
  }

  def addApi(applicationId: String, apiId: String, availableEndpoints: Seq[AvailableEndpoint])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val scopes = availableEndpoints.flatMap(ae => ae.endpointMethod.scopes)
    val endpoints = availableEndpoints.map(ae => AddApiRequestEndpoint(ae.endpointMethod.httpMethod, ae.path))
    applicationsConnector.addApi(applicationId, AddApiRequest(apiId, endpoints, scopes))
  }

  def getUserApplications(email:String, enrich: Boolean = false)(implicit hc:HeaderCarrier): Future[Seq[Application]] =
    applicationsConnector.getUserApplications(email, enrich)

  def testConnectivity()(implicit hc: HeaderCarrier): Future[String] = {
    applicationsConnector.testConnectivity()
  }

  def getApiDetail(id: String)(implicit hc: HeaderCarrier): Future[Option[ApiDetail]] = {
    integrationCatalogueConnector.getApiDetail(id)
  }

  def getApiDeploymentStatuses(publisherReference: String)(implicit hc: HeaderCarrier): Future[Option[ApiDeploymentStatuses]] = {
    applicationsConnector.getApiDeploymentStatuses(publisherReference)
  }

  def getAllHipApis()(implicit hc: HeaderCarrier): Future[Seq[ApiDetail]] = {
    integrationCatalogueConnector.getAllHipApis()
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

  def getAccessRequests(applicationId: Option[String], status: Option[AccessRequestStatus])(implicit hc:HeaderCarrier): Future[Seq[AccessRequest]] = {
    applicationsConnector.getAccessRequests(applicationId, status)
  }

  def getAccessRequest(id: String)(implicit hc:HeaderCarrier): Future[Option[AccessRequest]] = {
    applicationsConnector.getAccessRequest(id)
  }

  def approveAccessRequest(id: String, decidedBy: String)(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.approveAccessRequest(id, decidedBy)
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

  def createTeam(team: NewTeam)(implicit hc: HeaderCarrier): Future[Unit] = {
    applicationsConnector.createTeam(team)
  }

}
