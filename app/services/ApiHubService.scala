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
import connectors.ApplicationsConnector
import models.application.{Application, NewApplication, NewScope, Secret}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
@Singleton
class ApiHubService @Inject()(applicationsConnector: ApplicationsConnector)
  extends Logging {

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    logger.debug(s"Registering application named '${newApplication.name}', created by user with email '${newApplication.createdBy.email}''")
    applicationsConnector.registerApplication(newApplication)
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.getApplications()
  }

  def getApplication(id:String)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    applicationsConnector.getApplication(id)
  }

  def requestAdditionalScope(id:String, newScope:NewScope)(implicit hc:HeaderCarrier): Future[Option[NewScope]] = {
    logger.debug(s"Requesting scope named '${newScope.name}' for application id '$id' in environments ${newScope.environments}")
    applicationsConnector.requestAdditionalScope(id, newScope)
  }

  def pendingPrimaryScopes()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.pendingPrimaryScopes()
  }

  def approveProductionScope(appId: String, scopeName: String)(implicit hc:HeaderCarrier): Future[Boolean] = {
    logger.debug(s"Approving scope named '$scopeName' for application id '$appId' in PROD environment")
    applicationsConnector.approveProductionScope(appId, scopeName)
  }

  def getUserApplications(email:String)(implicit hc:HeaderCarrier): Future[Seq[Application]] =
        applicationsConnector.getUserApplications(email)

  def createPrimarySecret(id: String)(implicit hc:HeaderCarrier): Future[Option[Secret]] = {
    logger.debug(s"Creating primary secret for application $id")
    applicationsConnector.createPrimarySecret(id)
  }

}
