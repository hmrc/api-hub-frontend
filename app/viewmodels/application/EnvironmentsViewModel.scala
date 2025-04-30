/*
 * Copyright 2024 HM Revenue & Customs
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

package viewmodels.application

import config.HipEnvironment
import models.api.ApiDeploymentStatus
import models.application.{Application, Credential}
import models.user.UserModel

case class CredentialsTabViewModel(credentials: Seq[Credential], applicationId: String, hipEnvironment: HipEnvironment, user: UserModel, errorRetrievingCredentials: Boolean, noApis: Boolean, apiHubGuideUrl: String) {
  val userCanAddCredentials = !hipEnvironment.isProductionLike || user.permissions.isPrivileged

  val maxCredentialsReached = credentials.size >= 5

  val userCanDeleteCredentials = credentials.size > 1 && userCanAddCredentials

  val addCredentialFormAction = hipEnvironment.isProductionLike match {
    case true => controllers.application.routes.AddCredentialController.checklist(applicationId, hipEnvironment.id)
    case false => controllers.application.routes.AddCredentialController.addCredentialForEnvironment(applicationId, hipEnvironment.id)
  }

  val showNoProductionCredentialsMessage = hipEnvironment.isProductionLike && credentials.isEmpty
}

case class ApiTabViewModel(applicationApis: Seq[ApplicationApi], applicationId: String, hipEnvironment: HipEnvironment, noApis: Boolean, deployedApiVersions: Map[String, ApiDeploymentStatus]) {
  val showRequestProdAccessBanner = hipEnvironment.isProductionLike && applicationApis.exists(_.needsAccessRequest(hipEnvironment))
  val pendingAccessRequestsCount = applicationApis.count(_.hasPendingAccessRequest(hipEnvironment))
  val showPendingAccessRequestsBanner = hipEnvironment.isProductionLike && pendingAccessRequestsCount > 0
  def getApiVersion(apiId: String): Option[String] = {
    deployedApiVersions.get(apiId) match {
      case Some(ApiDeploymentStatus.Deployed(_, version)) => Some(version)
      case _ => None
    }}
}

case class EnvironmentsViewModel(application: Application, applicationApis: Seq[ApplicationApi], user: UserModel, hipEnvironment: HipEnvironment, credentials: Seq[Credential], apiHubGuideUrl: String, errorRetrievingCredentials: Boolean = false, deployedApiVersions: Map[String, ApiDeploymentStatus]) {
  val credentialsTabViewModel = CredentialsTabViewModel(credentials, application.id, hipEnvironment, user, errorRetrievingCredentials, applicationApis.isEmpty, apiHubGuideUrl)
  val apiTabViewModel = ApiTabViewModel(applicationApis, application.id, hipEnvironment, applicationApis.isEmpty, deployedApiVersions)
}
