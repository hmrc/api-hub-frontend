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
import controllers.helpers.ApplicationApiBuilder
import models.AvailableEndpoint
import models.accessrequest.{AccessRequest, AccessRequestApi, AccessRequestEndpoint, AccessRequestRequest, AccessRequestStatus}
import models.api.ApiDetail
import models.application._
import models.exception.ApplicationsException
import models.requests.{AddApiRequest, AddApiRequestEndpoint, DataRequest}
import play.api.Logging
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ApiHubService @Inject()(
  applicationsConnector: ApplicationsConnector,
  integrationCatalogueConnector: IntegrationCatalogueConnector) extends Logging {

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

  def requestAdditionalScope(id:String, newScope:NewScope)(implicit hc:HeaderCarrier): Future[Option[NewScope]] = {
    logger.debug(s"Requesting scope named '${newScope.name}' for application id '$id' in environments ${newScope.environments}")
    applicationsConnector.requestAdditionalScope(id, newScope)
  }

  def addScopes(id: String, scopeNames: Set[String])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    applicationsConnector.addScopes(id, scopeNames.map(name => NewScope(name, Seq(Secondary))).toSeq)
  }

  def addApi(applicationId: String, apiId: String, availableEndpoints: Seq[AvailableEndpoint])(implicit hc: HeaderCarrier): Future[Option[Unit]] = {
    val scopes = availableEndpoints.flatMap(ae => ae.endpointMethod.scopes)
    val endpoints = availableEndpoints.map(ae => AddApiRequestEndpoint(ae.endpointMethod.httpMethod, ae.path))
    applicationsConnector.addApi(applicationId, AddApiRequest(apiId, endpoints, scopes))
  }

  def pendingPrimaryScopes()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.pendingPrimaryScopes()
  }

  def approvePrimaryScope(appId: String, scopeName: String)(implicit hc:HeaderCarrier): Future[Boolean] = {
    logger.debug(s"Approving scope named '$scopeName' for application id '$appId' in primary environment")
    applicationsConnector.approvePrimaryScope(appId, scopeName)
  }

  def getUserApplications(email:String, enrich: Boolean = false)(implicit hc:HeaderCarrier): Future[Seq[Application]] =
    applicationsConnector.getUserApplications(email, enrich)

  def createPrimarySecret(id: String)(implicit hc:HeaderCarrier): Future[Option[Secret]] = {
    logger.debug(s"Creating primary secret for application $id")
    applicationsConnector.createPrimarySecret(id)
  }

  def testConnectivity()(implicit hc: HeaderCarrier): Future[String] = {
    applicationsConnector.testConnectivity()
  }

  def getApiDetail(id: String)(implicit hc: HeaderCarrier): Future[Option[ApiDetail]] = {
    integrationCatalogueConnector.getApiDetail(id)
  }

  def getAllHipApis()(implicit hc: HeaderCarrier): Future[Seq[ApiDetail]] = {
    integrationCatalogueConnector.getAllHipApis()
  }

  def addCredential(id: String, environmentName: EnvironmentName)(implicit hc: HeaderCarrier): Future[Either[ApplicationsException, Option[Credential]]] = {
    applicationsConnector.addCredential(id, environmentName)
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

  def requestProductionAccess(accessRequest: AccessRequestRequest)(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Unit] = {
    applicationsConnector.createAccessRequest(accessRequest)
  }
}
