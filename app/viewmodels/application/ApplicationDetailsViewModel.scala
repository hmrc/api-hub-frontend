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
import models.application.Application
import models.user.UserModel

case class ApplicationDetailsViewModel(application: Application, applicationApis: Seq[ApplicationApi], user: Option[UserModel], productionEnvironmentId: String) {
  val applicationId = application.id
  val applicationName = application.name
  val showApplicationProblemsPanel = application.issues.nonEmpty
  val apiCount = applicationApis.size
  val noApis = apiCount == 0
  val missingApiNames: Seq[String] = applicationApis.filter(_.isMissing).map(_.apiTitle)
  val allApiNames = applicationApis.map(_.apiTitle)
  val hasMissingApis = missingApiNames.nonEmpty
  val pendingAccessRequestsCount = applicationApis.map(_.pendingAccessRequestCount).sum
  val hasPendingAccessRequests = pendingAccessRequestsCount > 0
  val needsProductionAccessRequest = applicationApis.exists(_.needsProductionAccessRequest)
  val notUsingGlobalTeams = application.teamId.isEmpty
  val applicationTeamMemberCount = application.teamMembers.size
  val applicationTeamMemberEmails = application.teamMembers.map(_.email)
}
