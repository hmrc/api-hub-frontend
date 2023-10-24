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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import models.Lens
import models.application.ApplicationLenses._
import models.application.ApplicationLensesSpec._

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.util.Random

class ApplicationLensesSpec extends AnyFreeSpec with Matchers with LensBehaviours {

  "applicationEnvironments" - {
    "must get the correct Environments" in {
      val expected = randomEnvironments()
      val application = testApplication.copy(environments = expected)

      val actual = applicationEnvironments.get(application)
      actual mustBe expected
    }

    "must set the Environments correctly" in {
      val expected = randomEnvironments()
      val application = applicationEnvironments.set(testApplication, expected)

      application.environments mustBe expected
    }
  }

  "environmentScopes" - {
    "must get the correct Scopes" in {
      val expected = randomScopes()
      val environment = randomEnvironment().copy(scopes = expected)

      val actual = environmentScopes.get(environment)
      actual mustBe expected
    }

    "must set the scopes correctly" in {
      val expected = randomScopes()
      val environment = randomEnvironment().copy(scopes = Seq.empty)

      val actual = environmentScopes.set(environment, expected).scopes
      actual mustBe expected
    }
  }

  "environmentCredentials" - {
    "must get the correct credentials" in {
      val expected = randomCredentials()
      val environment = randomEnvironment().copy(credentials = expected)

      val actual = environmentCredentials.get(environment)
      actual mustBe expected
    }

    "must set the credentials correctly" in {
      val expected = randomCredentials()
      val environment = randomEnvironment().copy(credentials = Seq.empty)

      val actual = environmentCredentials.set(environment, expected).credentials
      actual mustBe expected
    }
  }

  "environmentPrimary" - {
    "must" - {
      behave like environmentsToEnvironmentLens(
        environmentPrimary,
        _.primary
      )
    }
  }

  "applicationPrimary" - {
    "must" - {
      behave like applicationToEnvironmentLens(
        applicationPrimary,
        _.primary
      )
    }
  }

  "applicationPrimaryScopes" - {
    "must" - {
      behave like applicationToScopesLens(
        applicationPrimaryScopes,
        _.primary
      )
    }
  }

  "applicationPrimaryCredentials" - {
    "must" - {
      behave like applicationToCredentialsLens(
        applicationPrimaryCredentials,
        _.primary
      )
    }
  }

  "environmentSecondary" - {
    "must" - {
      behave like environmentsToEnvironmentLens(
        environmentSecondary,
        _.secondary
      )
    }
  }

  "applicationSecondary" - {
    "must" - {
      behave like applicationToEnvironmentLens(
        applicationSecondary,
        _.secondary
      )
    }
  }

  "applicationSecondaryScopes" - {
    "must" - {
      behave like applicationToScopesLens(
        applicationSecondaryScopes,
        _.secondary
      )
    }
  }

  "applicationSecondaryCredentials" - {
    "must" - {
      behave like applicationToCredentialsLens(
        applicationSecondaryCredentials,
        _.secondary
      )
    }
  }

  "applicationTeamMembers" - {
    "must get the correct team members" in {
      val application = testApplication.copy(teamMembers = randomTeamMembers())
      val actual = applicationTeamMembers.get(application)
      actual mustBe application.teamMembers
    }

    "must set the team members correctly" in {
      val application = testApplication.copy(teamMembers = randomTeamMembers())
      val expected = randomTeamMembers()
      val actual = applicationTeamMembers.set(application, expected).teamMembers
      actual mustBe expected
    }
  }

  "ApplicationLensOps" - {
    "getPrimaryScopes" - {
      "must" - {
        behave like applicationScopesGetterFunction(
          applicationPrimaryScopes,
          application => ApplicationLensOps(application).getPrimaryScopes
        )
      }
    }

    "setPrimaryScopes" - {
      "must" - {
        behave like applicationScopesSetterFunction(
          applicationPrimaryScopes,
          (application, scopes) => ApplicationLensOps(application).setPrimaryScopes(scopes)
        )
      }
    }

    "addPrimaryScope" - {
      "must" - {
        behave like applicationAddScopeFunction(
          applicationPrimaryScopes,
          (application, scope) => ApplicationLensOps(application).addPrimaryScope(scope)
        )
      }
    }

    "hasPrimaryPendingScope" - {
      "must return true when the application has a primary pending scope" in {
        val application = testApplication.addPrimaryScope(Scope("test-scope", Pending))
        application.hasPrimaryPendingScope mustBe true
      }

      "must return false when the application does not have a primary pending scope" in {
        val application = testApplication
          .addPrimaryScope(Scope(randomString(), Approved))
          .addPrimaryScope(Scope(randomString(), Denied))
          .addSecondaryScope(Scope(randomString(), Pending))

        application.hasPrimaryPendingScope mustBe false
      }
    }

    "getPrimaryMasterCredential" - {
      "must return the most recently created credential" in {
        val master = randomCredential().copy(created = LocalDateTime.now())
        val credential1 = randomCredential().copy(created = LocalDateTime.now().minusDays(1))
        val credential2 = randomCredential().copy(created = LocalDateTime.now().minusDays(2))

        val application = testApplication
          .setPrimaryCredentials(Seq(credential1, master, credential2))

        application.getPrimaryMasterCredential mustBe master
      }
    }

    "getPrimaryCredentials" - {
      "must" - {
        behave like applicationCredentialsGetterFunction(
          applicationPrimaryCredentials,
          application => ApplicationLensOps(application).getPrimaryCredentials
        )
      }
    }

    "setPrimaryCredentials" - {
      "must" - {
        behave like applicationCredentialsSetterFunction(
          applicationPrimaryCredentials,
          (application, credentials) => ApplicationLensOps(application).setPrimaryCredentials(credentials)
        )
      }
    }

    "addPrimaryCredential" - {
      "must" - {
        behave like applicationAddCredentialFunction(
          applicationPrimaryCredentials,
          (application, credential) => ApplicationLensOps(application).addPrimaryCredential(credential)
        )
      }
    }

    "getSecondaryScopes" - {
      "must" - {
        behave like applicationScopesGetterFunction(
          applicationSecondaryScopes,
          application => ApplicationLensOps(application).getSecondaryScopes
        )
      }
    }

    "setSecondaryScopes" - {
      "must" - {
        behave like applicationScopesSetterFunction(
          applicationSecondaryScopes,
          (application, scopes) => ApplicationLensOps(application).setSecondaryScopes(scopes)
        )
      }
    }

    "addSecondaryScope" - {
      "must" - {
        behave like applicationAddScopeFunction(
          applicationSecondaryScopes,
          (application, scope) => ApplicationLensOps(application).addSecondaryScope(scope)
        )
      }
    }

    "getSecondaryMasterCredential" - {
      "must return the most recently created credential" in {
        val master = randomCredential().copy(created = LocalDateTime.now())
        val credential1 = randomCredential().copy(created = LocalDateTime.now().minusDays(1))
        val credential2 = randomCredential().copy(created = LocalDateTime.now().minusDays(2))

        val application = testApplication
          .setSecondaryCredentials(Seq(credential1, master, credential2))

        application.getSecondaryMasterCredential mustBe master
      }
    }

    "getSecondaryCredentials" - {
      "must" - {
        behave like applicationCredentialsGetterFunction(
          applicationSecondaryCredentials,
          application => ApplicationLensOps(application).getSecondaryCredentials
        )
      }
    }

    "setSecondaryCredentials" - {
      "must" - {
        behave like applicationCredentialsSetterFunction(
          applicationSecondaryCredentials,
          (application, credentials) => ApplicationLensOps(application).setSecondaryCredentials(credentials)
        )
      }
    }

    "addSecondaryCredential" - {
      "must" - {
        behave like applicationAddCredentialFunction(
          applicationSecondaryCredentials,
          (application, credential) => ApplicationLensOps(application).addSecondaryCredential(credential)
        )
      }
    }

    "hasTeamMember" - {
      "must return true when the given email address belongs to a team member" in {
        val application = testApplication.copy(
          teamMembers = Seq(
            TeamMember("JoBlOgGs@EmAiL.cOm"),
            TeamMember("notjobloggs@email.com")
          )
        )

        application.hasTeamMember("jobloggs@email.com") mustBe true
      }

      "must return false when the given email does not belong to a team member" in {
        val application = testApplication.copy(
          teamMembers = Seq(
            TeamMember("team-member-1@email.com"),
            TeamMember("team-member-2@email.com")
          )
        )

        application.hasTeamMember("team-member-3@email.com") mustBe false
      }
    }

    "addTeamMember" - {
      "must add a team member with the given email to the application" in {
        val existing = Seq(
          TeamMember("team-member-1@email.com"),
          TeamMember("team-member-2@email.com")
        )

        val application = testApplication.copy(teamMembers = existing)
        val added = TeamMember(email = "team-member-3@email.com")

        val actual = application.addTeamMember(added.email).teamMembers
        actual mustBe existing :+ added
      }
    }

    "getRequiredScopeNames" - {
      "must return the set of all secondary scopes" in {
        val scopes = Seq(
          Scope("test-scope-1", Approved),
          Scope("test-scope-1", Approved),
          Scope("test-scope-2", Approved)
        )

        val application = testApplication.setSecondaryScopes(scopes)

        val actual = application.getRequiredScopeNames
        actual must contain theSameElementsAs Set("test-scope-1", "test-scope-2")
      }

      "must return an empty set when there are no secondary scopes" in {
        val application = testApplication.setSecondaryScopes(Seq.empty)

        val actual = application.getRequiredScopeNames
        actual mustBe empty
      }
    }
  }

}

//noinspection ScalaStyle
object ApplicationLensesSpec {

  private val testApplication: Application = Application("test-id", "test-name", Creator("test-email"), Seq(TeamMember("test-email")))
  private val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  def randomEnvironments(): Environments = Environments(
    primary = Environment(),
    secondary = Environment()
  )

  private def randomEnvironment(): Environment =
    Environment(
      scopes = randomScopes(),
      credentials = randomCredentials()
    )

  private def randomCredentials(): Seq[Credential] =
    (0 to Random.nextInt(5))
      .map(_ => randomCredential())

  private def randomCredential(): Credential = {
    val clientSecret = s"test-client-secret${randomString()}"
    Credential(
      clientId = s"test-client-id${randomString()}",
      created = LocalDateTime.now(clock),
      clientSecret = Some(clientSecret),
      secretFragment = Some(clientSecret.takeRight(4))
    )
  }

  private def randomScopes(): Seq[Scope] =
    (0 to Random.nextInt(5))
      .map(_ => randomScope())

  private def randomScope(): Scope =
    Scope(
      name = s"test-scope${randomString()}",
      status = randomScopeStatus()
    )

  private def randomScopeStatus(): ScopeStatus =
    Random.nextInt(3) match {
      case 0 => Pending
      case 1 => Denied
      case _ => Approved
    }

  private def randomTeamMember(): TeamMember =
    TeamMember(email = randomString())

  private def randomTeamMembers(): Seq[TeamMember] =
    (0 to Random.nextInt(5))
      .map(_ => randomTeamMember())

  private def randomString(): String = Random.alphanumeric.take(Random.nextInt(10) + 1).mkString

  trait LensBehaviours {
    this: AnyFreeSpec with Matchers =>

    def environmentsToEnvironmentLens(
                                       lens: Lens[Environments, Environment],
                                       getEnvironment: Environments => Environment
                                     ): Unit = {
      "it must get the correct environment" in {
        val environments = randomEnvironments()
        val expected = getEnvironment(environments)

        val actual = lens.get(environments)
        actual mustBe expected
      }

      "it must set the environment correctly" in {
        val environments = Environments()
        val expected = randomEnvironment()

        val actual = getEnvironment(lens.set(environments, expected))
        actual mustBe expected
      }
    }

    def applicationToEnvironmentLens(
                                      lens: Lens[Application, Environment],
                                      getEnvironment: Environments => Environment
                                    ): Unit = {
      "it must get the correct environment" in {
        val application = testApplication.copy(environments = randomEnvironments())
        val expected = getEnvironment(application.environments)

        val actual = lens.get(application)
        actual mustBe expected
      }

      "it must set the environment correctly" in {
        val application = testApplication
        val expected = randomEnvironment()

        val actual = getEnvironment(lens.set(application, expected).environments)
        actual mustBe expected
      }
    }

    def applicationToScopesLens(
                                 lens: Lens[Application, Seq[Scope]],
                                 getsEnvironment: Environments => Environment
                               ): Unit = {
      "it must get the correct scopes" in {
        val application = testApplication.copy(environments = randomEnvironments())
        val expected = getsEnvironment(application.environments).scopes

        val actual = lens.get(application)
        actual mustBe expected
      }

      "it must set the scopes correctly" in {
        val application = testApplication
        val expected = randomScopes()

        val actual = getsEnvironment(lens.set(application, expected).environments).scopes
        actual mustBe expected
      }
    }

    def applicationToCredentialsLens(
                                      lens: Lens[Application, Seq[Credential]],
                                      getsEnvironment: Environments => Environment
                                    ): Unit = {
      "it must get the correct credentials" in {
        val application = testApplication.copy(environments = randomEnvironments())
        val expected = getsEnvironment(application.environments).credentials

        val actual = lens.get(application)
        actual mustBe expected
      }

      "it must set the credentials correctly" in {
        val application = testApplication
        val expected = randomCredentials()

        val actual = getsEnvironment(lens.set(application, expected).environments).credentials
        actual mustBe expected
      }
    }

    def applicationScopesGetterFunction(
                                         lens: Lens[Application, Seq[Scope]],
                                         getsScopes: Application => Seq[Scope]
                                       ): Unit = {
      "it must get the correct scopes" in {
        val expected = randomScopes()
        val actual = getsScopes(lens.set(testApplication, expected))
        actual mustBe expected
      }
    }

    def applicationScopesSetterFunction(
                                         lens: Lens[Application, Seq[Scope]],
                                         setsScopes: (Application, Seq[Scope]) => Application
                                       ): Unit = {
      "must set the scopes correctly" in {
        val expected = randomScopes()
        val actual = lens.get(setsScopes(testApplication, expected))
        actual mustBe expected
      }
    }

    def applicationAddScopeFunction(
                                     lens: Lens[Application, Seq[Scope]],
                                     addsScope: (Application, Scope) => Application
                                   ): Unit = {
      "must add the scope correctly" in {
        val scopes = randomScopes()
        val newScope = randomScope()
        val expected = scopes :+ newScope
        val application = lens.set(testApplication, scopes)

        val actual = lens.get(addsScope(application, newScope))
        actual mustBe expected
      }
    }

    def applicationCredentialsGetterFunction(
                                              lens: Lens[Application, Seq[Credential]],
                                              getsCredentials: Application => Seq[Credential]
                                            ): Unit = {
      "it must get the correct credentials" in {
        val expected = randomCredentials()
        val actual = getsCredentials(lens.set(testApplication, expected))
        actual mustBe expected
      }
    }

    def applicationCredentialsSetterFunction(
                                              lens: Lens[Application, Seq[Credential]],
                                              setsCredentials: (Application, Seq[Credential]) => Application
                                            ): Unit = {
      "must set the credentials correctly" in {
        val expected = randomCredentials()
        val actual = lens.get(setsCredentials(testApplication, expected))
        actual mustBe expected
      }
    }

    def applicationAddCredentialFunction(
                                          lens: Lens[Application, Seq[Credential]],
                                          addsCredential: (Application, Credential) => Application
                                        ): Unit = {
      "must add the credential correctly" in {
        val credentials = randomCredentials()
        val newCredential = randomCredential()
        val expected = credentials :+ newCredential
        val application = lens.set(testApplication, credentials)

        val actual = lens.get(addsCredential(application, newCredential))
        actual mustBe expected
      }
    }
  }

}
