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

import fakes.FakeHipEnvironments
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import models.Lens
import models.application.ApplicationLenses.*
import models.application.ApplicationLensesSpec.*

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.util.Random

class ApplicationLensesSpec extends LensBehaviours {

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

  "applicationApis" - {
    "must get the correct APIs" in {
      val application = testApplication.copy(apis = randomApis())
      val actual = applicationApis.get(application)
      actual mustBe application.apis
    }

    "must set the APIs correctly" in {
      val application = testApplication.copy(apis = randomApis())
      val expected = randomApis()
      val actual = applicationApis.set(application, expected).apis
      actual mustBe expected
    }
  }

  "ApplicationLensOps" - {
    "getCredentials" - {
      "must" - {
        behave like applicationCredentialsGetterFunction(
          applicationCredentials,
          application => ApplicationLensOps(application).getCredentials(FakeHipEnvironments.production).toSet,
          FakeHipEnvironments.production.id
        )
      }

      "must also" - {
        behave like applicationCredentialsGetterFunction(
          applicationCredentials,
          application => ApplicationLensOps(application).getCredentials(FakeHipEnvironments.test).toSet,
          FakeHipEnvironments.test.id
        )
      }
    }

    "setCredentials" - {
      "must" - {
        behave like applicationCredentialsSetterFunction(
          applicationCredentials,
          (application, credentials) => ApplicationLensOps(application).setCredentials(FakeHipEnvironments.production, credentials.toSeq),
          FakeHipEnvironments.production.id
        )
      }

      "must also" - {
        behave like applicationCredentialsSetterFunction(
          applicationCredentials,
          (application, credentials) => ApplicationLensOps(application).setCredentials(FakeHipEnvironments.test, credentials.toSeq),
          FakeHipEnvironments.test.id
        )
      }
    }

    "getMasterCredential" - {
      "must return the most recently created primary credential" in {
        val master = randomCredential(FakeHipEnvironments.production.id).copy(created = LocalDateTime.now())
        val credential1 = randomCredential(FakeHipEnvironments.production.id).copy(created = LocalDateTime.now().minusDays(1))
        val credential2 = randomCredential(FakeHipEnvironments.production.id).copy(created = LocalDateTime.now().minusDays(2))

        val application = testApplication
          .setCredentials(FakeHipEnvironments.production, Seq(credential1, master, credential2))

        application.getMasterCredential(FakeHipEnvironments.production) mustBe Some(master)
      }

      "must return the most recently created secondary credential" in {
        val master = randomCredential(FakeHipEnvironments.test.id).copy(created = LocalDateTime.now())
        val credential1 = randomCredential(FakeHipEnvironments.test.id).copy(created = LocalDateTime.now().minusDays(1))
        val credential2 = randomCredential(FakeHipEnvironments.test.id).copy(created = LocalDateTime.now().minusDays(2))

        val application = testApplication
          .setCredentials(FakeHipEnvironments.test, Seq(credential1, master, credential2))

        application.getMasterCredential(FakeHipEnvironments.test) mustBe Some(master)
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

    "addApi" - {
      "must add the API to the application" in {
        val existing = randomApis()
        val application = testApplication.copy(apis = existing)
        val added = randomApi()
        val actual = application.addApi(added).apis
        actual mustBe existing :+ added
      }
    }
  }

}

//noinspection ScalaStyle
object ApplicationLensesSpec {

  private val testApplication: Application = Application("test-id", "test-name", Creator("test-email"), Seq(TeamMember("test-email")))
  private val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  private def randomCredentials(environmentId: String): Set[Credential] =
    (0 to Random.nextInt(5))
      .map(_ => randomCredential(environmentId)).toSet

  private def randomCredential(environmentId: String): Credential = {
    val clientSecret = s"test-client-secret${randomString()}"
    Credential(
      clientId = s"test-client-id${randomString()}",
      created = LocalDateTime.now(clock),
      clientSecret = Some(clientSecret),
      secretFragment = Some(clientSecret.takeRight(4)),
      environmentId = environmentId
    )
  }

  private def randomTeamMember(): TeamMember =
    TeamMember(email = randomString())

  private def randomTeamMembers(): Seq[TeamMember] =
    (0 to Random.nextInt(5))
      .map(_ => randomTeamMember())

  private def randomEndpoint(): SelectedEndpoint =
    SelectedEndpoint(randomString(), randomString())

  private def randomEndpoints(): Seq[SelectedEndpoint] =
    (0 to Random.nextInt(5))
      .map(_ => randomEndpoint())

  private def randomApi(): Api =
    Api(randomString(), randomString(), randomEndpoints())

  private def randomApis(): Seq[Api] =
    (0 to Random.nextInt(5))
      .map(_ => randomApi())

  private def randomString(): String = Random.alphanumeric.take(Random.nextInt(10) + 1).mkString

  trait LensBehaviours extends AnyFreeSpec with Matchers {

    def applicationCredentialsGetterFunction(
                                              lens: Lens[Application, Set[Credential]],
                                              getsCredentials: Application => Set[Credential],
                                              environmentId: String
                                            ): Unit = {
      "it must get the correct credentials" in {
        val expected = randomCredentials(environmentId)
        val actual = getsCredentials(lens.set(testApplication, expected))
        actual mustBe expected
      }
    }

    def applicationCredentialsSetterFunction(
                                              lens: Lens[Application, Set[Credential]],
                                              setsCredentials: (Application, Set[Credential]) => Application,
                                              environmentId: String
                                            ): Unit = {
      "must set the credentials correctly" in {
        val expected = randomCredentials(environmentId)
        val actual = lens.get(setsCredentials(testApplication, expected))
        actual mustBe expected
      }
    }

    def applicationAddCredentialFunction(
                                          lens: Lens[Application, Set[Credential]],
                                          addsCredential: (Application, Credential) => Application,
                                          environmentId: String
                                        ): Unit = {
      "must add the credential correctly" in {
        val credentials = randomCredentials(environmentId)
        val newCredential = randomCredential(environmentId)
        val expected = credentials + newCredential
        val application = lens.set(testApplication, credentials)

        val actual = lens.get(addsCredential(application, newCredential))
        actual mustBe expected
      }
    }
  }

}
