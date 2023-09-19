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

object ApplicationLenses {

  val applicationEnvironments: Lens[Application, Environments] =
    Lens[Application, Environments](
      get = _.environments,
      set = (application, environments) => application.copy(environments = environments)
    )

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

  val environmentPrimary: Lens[Environments, Environment] =
    Lens[Environments, Environment](
      get = _.primary,
      set = (environments, primary) => environments.copy(primary = primary)
    )

  val applicationPrimary: Lens[Application, Environment] =
    Lens.compose(applicationEnvironments, environmentPrimary)

  val applicationPrimaryScopes: Lens[Application, Seq[Scope]] =
    Lens.compose(applicationPrimary, environmentScopes)

  val applicationPrimaryCredentials: Lens[Application, Seq[Credential]] =
    Lens.compose(applicationPrimary, environmentCredentials)

  val environmentSecondary: Lens[Environments, Environment] =
    Lens[Environments, Environment](
      get = _.secondary,
      set = (environments, secondary) => environments.copy(secondary = secondary)
    )

  val applicationSecondary: Lens[Application, Environment] =
    Lens.compose(applicationEnvironments, environmentSecondary)

  val applicationSecondaryScopes: Lens[Application, Seq[Scope]] =
    Lens.compose(applicationSecondary, environmentScopes)

  val applicationSecondaryCredentials: Lens[Application, Seq[Credential]] =
    Lens.compose(applicationSecondary, environmentCredentials)

  val applicationTeamMembers: Lens[Application, Seq[TeamMember]] =
    Lens[Application, Seq[TeamMember]](
      get = _.teamMembers,
      set = (application, teamMembers) => application.copy(teamMembers = teamMembers)
    )

  implicit class ApplicationLensOps(application: Application) {

    def getPrimaryScopes: Seq[Scope] =
      applicationPrimaryScopes.get(application)

    def setPrimaryScopes(scopes: Seq[Scope]): Application =
      applicationPrimaryScopes.set(application, scopes)

    def addPrimaryScope(scope: Scope): Application =
      applicationPrimaryScopes.set(
        application,
        applicationPrimaryScopes.get(application) :+ scope
      )

    def hasPrimaryPendingScope: Boolean =
      applicationPrimaryScopes.get(application)
        .exists(scope => scope.status == Pending)

    def getPrimaryCredentials: Seq[Credential] =
      applicationPrimaryCredentials.get(application)

    def setPrimaryCredentials(credentials: Seq[Credential]): Application =
      applicationPrimaryCredentials.set(application, credentials)

    def addPrimaryCredential(credential: Credential): Application =
      applicationPrimaryCredentials.set(
        application,
        applicationPrimaryCredentials.get(application) :+ credential
      )

    def getSecondaryScopes: Seq[Scope] =
      applicationSecondaryScopes.get(application)

    def setSecondaryScopes(scopes: Seq[Scope]): Application =
      applicationSecondaryScopes.set(application, scopes)

    def addSecondaryScope(scope: Scope): Application =
      applicationSecondaryScopes.set(
        application,
        applicationSecondaryScopes.get(application) :+ scope
      )

    def getSecondaryCredentials: Seq[Credential] =
      applicationSecondaryCredentials.get(application)

    def setSecondaryCredentials(credentials: Seq[Credential]): Application =
      applicationSecondaryCredentials.set(application, credentials)

    def addSecondaryCredential(credential: Credential): Application =
      applicationSecondaryCredentials.set(
        application,
        applicationSecondaryCredentials.get(application) :+ credential
      )

    def hasTeamMember(email: String): Boolean =
      applicationTeamMembers.get(application)
        .exists(teamMember => teamMember.email.equalsIgnoreCase(email))

    def addTeamMember(email: String): Application =
      applicationTeamMembers.set(
        application,
        applicationTeamMembers.get(application) :+ TeamMember(email)
      )

    def getRequiredScopeNames: Set[String] = {
      application
        .getSecondaryScopes
        .map(_.name)
        .toSet
    }

  }

}
