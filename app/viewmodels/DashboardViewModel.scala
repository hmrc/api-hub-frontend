/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels

import config.{FrontendAppConfig, V2Features}
import models.api.ApiDetail
import models.application.Application
import models.team.Team
import models.user.UserModel
import play.api.i18n.Messages

class DashboardViewModel(
  frontendAppConfig: FrontendAppConfig,
  applications: Seq[Application],
  teams: Seq[Team],
  apis: Seq[ApiDetail],
  val user: UserModel
) {

  def myApplicationsTitle(implicit messages: Messages): String = {
    if (myApplicationsCount == 0) {
      messages("dashboard.applications.title.none")
    }
    else if (myApplicationsCount == 1) {
      messages("dashboard.applications.title.one")
    }
    else {
      messages("dashboard.applications.title.many", myApplicationsCount)
    }
  }

  def showMyApplicationsEmptyMessage: Boolean = myApplicationsCount == 0

  def createApplicationMessage(implicit messages: Messages): String = {
    if (myApplicationsCount == 0) {
      messages("dashboard.applications.createFirst")
    }
    else {
      messages("dashboard.applications.createAnother")
    }
  }

  def createApplicationButtonIsSecondary: Boolean = myApplicationsCount > 0

  def myApplications: Seq[Application] =
    applications
      .sortBy(_.created)
      .reverse
      .take(frontendAppConfig.dashboardApplicationsToShow)

  def myApplicationsCount: Int = applications.size

  def showMyApplicationsLink: Boolean = applications.size > frontendAppConfig.dashboardApplicationsToShow

  def myTeamsTitle(implicit messages: Messages): String = {
    if (myTeamsCount == 0) {
      messages("dashboard.teams.title.none")
    }
    else if (myTeamsCount == 1) {
      messages("dashboard.teams.title.one")
    }
    else {
      messages("dashboard.teams.title.many", myTeamsCount)
    }
  }

  def showMyTeamsEmptyMessage: Boolean = myTeamsCount == 0

  def createTeamMessage(implicit messages: Messages): String = {
    if (myTeamsCount == 0) {
      messages("dashboard.teams.createFirst")
    }
    else {
      messages("dashboard.teams.createAnother")
    }
  }

  def createTeamButtonIsSecondary: Boolean = myTeamsCount > 0

  def myTeams: Seq[Team] =
    teams
      .sortBy(_.created)
      .reverse
      .take(frontendAppConfig.dashboardTeamsToShow)

  def myTeamsCount: Int = teams.size

  def showMyTeamsLink: Boolean = teams.size > frontendAppConfig.dashboardTeamsToShow

  def showApisOnDashboard: Boolean = V2Features.userAccess(teams, frontendAppConfig)

  def myApisTitle(implicit messages: Messages): String = {
    if (myApisCount == 0) {
      messages("dashboard.apis.title.none")
    }
    else if (myApisCount == 1) {
      messages("dashboard.apis.title.one")
    }
    else {
      messages("dashboard.apis.title.many", myApisCount)
    }
  }

  def showMyApisEmptyMessage: Boolean = myApisCount == 0

  def createApiMessage(implicit messages: Messages): String = {
    if (myApisCount == 0) {
      messages("dashboard.apis.createFirst")
    }
    else {
      messages("dashboard.apis.createAnother")
    }
  }

  def createApisButtonIsSecondary: Boolean = myApisCount > 0

  def myApis: Seq[ApiDetail] =
    apis
      .take(frontendAppConfig.dashboardApisToShow)

  def myApisCount: Int = apis.size

  def showMyApisLink: Boolean = apis.size > frontendAppConfig.dashboardApisToShow

}
