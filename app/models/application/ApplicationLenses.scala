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

import models.Lens
import models.user.UserModel

object ApplicationLenses {

  // TODO: replace these hacks with something better
  private def setProductionEnvironment(application: Application, environment: Environment): Application = {
    application.copy(
      environments = application.environments.copy(primary = environment)
    )
  }

  private def setTestEnvironment(application: Application, environment: Environment): Application = {
    application.copy(
      environments = application.environments.copy(secondary = environment)
    )
  }

  val environmentScopes: Lens[Environment, Seq[Scope]] =
    Lens[Environment, Seq[Scope]](
      get = _.scopes,
      set = (environment, scopes) => environment.copy(scopes = scopes)
    )

  val environmentCredentials: Lens[Environment, Seq[Credential]] =
    Lens[Environment, Seq[Credential]](
      get = _.credentials,
      set = (environment, credentials) => environment.copy(credentials = credentials)
    )

  val applicationProduction: Lens[Application, Environment] =
    Lens[Application, Environment](
      get = _.newEnvironments.getOrElse(Production, Environment()),
      set = (application, environment) => setProductionEnvironment(application, environment)
    )

  val applicationProductionScopes: Lens[Application, Seq[Scope]] =
    Lens.compose(applicationProduction, environmentScopes)

  val applicationProductionCredentials: Lens[Application, Seq[Credential]] =
    Lens.compose(applicationProduction, environmentCredentials)

  val applicationTest: Lens[Application, Environment] =
    Lens[Application, Environment](
      get = _.newEnvironments.getOrElse(Test, Environment()),
      set = (application, environment) => setTestEnvironment(application, environment)
    )

  val applicationTestScopes: Lens[Application, Seq[Scope]] =
    Lens.compose(applicationTest, environmentScopes)

  val applicationTestCredentials: Lens[Application, Seq[Credential]] =
    Lens.compose(applicationTest, environmentCredentials)

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

    def getProductionScopes: Seq[Scope] =
      applicationProductionScopes.get(application)

    def setProductionScopes(scopes: Seq[Scope]): Application =
      applicationProductionScopes.set(application, scopes)

    def addProductionScope(scope: Scope): Application =
      applicationProductionScopes.set(
        application,
        applicationProductionScopes.get(application) :+ scope
      )

    def getProductionMasterCredential: Option[Credential] =
      applicationProductionCredentials.get(application)
        .sortWith((a, b) => a.created.isAfter(b.created))
        .headOption

    def getProductionCredentials: Seq[Credential] =
      applicationProductionCredentials.get(application)

    def setProductionCredentials(credentials: Seq[Credential]): Application =
      applicationProductionCredentials.set(application, credentials)

    def addProductionCredential(credential: Credential): Application =
      applicationProductionCredentials.set(
        application,
        applicationProductionCredentials.get(application) :+ credential
      )

    def getTestScopes: Seq[Scope] =
      applicationTestScopes.get(application)

    def setTestScopes(scopes: Seq[Scope]): Application =
      applicationTestScopes.set(application, scopes)

    def addTestScope(scope: Scope): Application =
      applicationTestScopes.set(
        application,
        applicationTestScopes.get(application) :+ scope
      )

    def getTestMasterCredential: Option[Credential] =
      applicationTestCredentials.get(application)
        .sortWith((a, b) => a.created.isAfter(b.created))
        .headOption

    def getTestCredentials: Seq[Credential] =
      applicationTestCredentials.get(application)

    def setTestCredentials(credentials: Seq[Credential]): Application =
      applicationTestCredentials.set(application, credentials)

    def addTestCredential(credential: Credential): Application =
      applicationTestCredentials.set(
        application,
        applicationTestCredentials.get(application) :+ credential
      )

    def getCredentialsFor(environmentName: EnvironmentName): Seq[Credential] = {
      environmentName match {
        case Production => application.getProductionCredentials
        case Test => application.getTestCredentials
        case _ => throw new IllegalArgumentException(s"Unsupported environment: $environmentName")  // TODO
      }
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

    def getRequiredScopeNames: Set[String] = {
      application
        .getTestScopes
        .map(_.name)
        .toSet
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
