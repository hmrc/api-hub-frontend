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

package models.application

import config.HipEnvironment
import models.Lens
import models.user.UserModel

object ApplicationLenses {

  val applicationCredentials: Lens[Application, Set[Credential]] =
    Lens[Application, Set[Credential]](
      get = _.credentials,
      set = (application, credentials) => application.copy(credentials = credentials)
    )

  val applicationTeamMembers: Lens[Application, Seq[TeamMember]] =
    Lens[Application, Seq[TeamMember]](
      get = _.teamMembers,
      set = (application, teamMembers) => application.copy(teamMembers = teamMembers)
    )

  val applicationApis: Lens[Application, Seq[Api]] =
    Lens[Application, Seq[Api]](
      get = _.apis,
      set = (application, apis) => application.copy(apis = apis)
    )

  val applicationDeleted: Lens[Application, Option[Deleted]] =
    Lens[Application, Option[Deleted]](
      get = _.deleted,
      set = (application, deleted) => application.copy(deleted = deleted)
    )

  implicit class ApplicationLensOps(application: Application) {

    private def getCredentials(environmentId: String): Seq[Credential] = {
      applicationCredentials
        .get(application)
        .filter(_.environmentId == environmentId)
        .toSeq
        .sortBy(_.created)
    }

    def getCredentials(hipEnvironment: HipEnvironment): Seq[Credential] = {
      getCredentials(hipEnvironment.id)
    }

    def getMasterCredential(hipEnvironment: HipEnvironment): Option[Credential] = {
      getCredentials(hipEnvironment)
        .sortWith((a, b) => a.created.isAfter(b.created))
        .headOption
    }

    private def setCredentials(environmentId: String, credentials: Seq[Credential]): Application = {
      applicationCredentials.set(
        application,
        applicationCredentials
          .get(application)
          .filterNot(_.environmentId == environmentId) ++ credentials.toSet
      )
    }

    def setCredentials(hipEnvironment: HipEnvironment, credentials: Seq[Credential]): Application = {
      setCredentials(hipEnvironment.id, credentials)
    }

    def setCredentials(credentials: Set[Credential]): Application = {
      applicationCredentials.set(application, credentials)
    }

    def setTeamId(teamId: String): Application = {
      application.copy(teamId = Some(teamId))
    }

    def setTeamName(teamName: String): Application =
      application.copy(teamName = Some(teamName))

    def hasTeam: Boolean =
      application.teamId.isDefined && application.teamName.isDefined

    def hasTeamMember(email: String): Boolean =
      applicationTeamMembers.get(application)
        .exists(teamMember => teamMember.email.equalsIgnoreCase(email))

    def hasTeamMember(teamMember: TeamMember): Boolean =
      hasTeamMember(teamMember.email)

    def hasTeamMember(user: UserModel): Boolean =
      application.hasTeamMember(user.email)

    def isAccessible(user: UserModel): Boolean =
      user.permissions.canSupport ||
        hasTeamMember(user.email)

    def addTeamMember(email: String): Application =
      applicationTeamMembers.set(
        application,
        applicationTeamMembers.get(application) :+ TeamMember(email)
      )

    def addTeamMember(user: UserModel): Application =
      addTeamMember(user.email)

    def setTeamMembers(teamMembers: Seq[TeamMember]): Application = {
      applicationTeamMembers.set(application, teamMembers)
    }

    def removeTeamMember(user: UserModel): Application =
      applicationTeamMembers.set(
        application,
        applicationTeamMembers.get(application).filterNot(_.email.toLowerCase.equals(user.email.toLowerCase))
      )

    def withSortedTeam(): Application =
      applicationTeamMembers.set(
        application,
        application.teamMembers.sortWith(_.email.toUpperCase() < _.email.toUpperCase())
      )

    def isTeamMigrated: Boolean = {
      application.teamId.isDefined
    }

    def addApi(api: Api): Application =
      applicationApis.set(
        application,
        applicationApis.get(application) :+ api
      )

    def hasApi(id: String): Boolean = {
      application.apis.exists(_.id.equals(id))
    }

    def delete(deleted: Deleted): Application =
      applicationDeleted.set(
        application,
        Some(deleted)
      )

    def isDeleted: Boolean = {
      application.deleted.isDefined
    }

  }

}
