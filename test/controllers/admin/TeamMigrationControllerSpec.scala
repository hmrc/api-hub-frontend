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

package controllers.admin

import base.SpecBase
import controllers.admin.TeamMigrationController.{MigrationSummary, TeamApplications}
import generators.TeamGenerator
import models.user.UserModel
import models.application.{Application, Creator, Deleted, TeamMember}
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.TeamMigrationView

import java.time.LocalDateTime
import scala.concurrent.Future

class TeamMigrationControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation
    with TeamGenerator {

  "TeamMigrationController" - {
    "must return Ok and the correct view for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val migratedApplication = Application(
          "id-1",
          "test-app-name-1",
          Creator("test-creator-email-1"),
          Seq(TeamMember("test-creator-email-1"))
        ).copy(teamId = Some("team-id"))
        val deletedApplication = 
          Application(
            "id-2",
            "test-app-name-2",
            Creator("test-creator-email-2"),
            Seq(TeamMember("test-creator-email-2"))
          ).copy(deleted = Some(Deleted(LocalDateTime.now, "test-deleter-email-2")))
        val nonMigratedApplication = Application(
          "id-3",
          "test-app-name-3",
          Creator("test-creator-email-3"),
          Seq(TeamMember("test-creator-email-3"))
        )
        val migratedDeletedApplication = Application(
          "id-4",
          "test-app-name-4",
          Creator("test-creator-email-4"),
          Seq(TeamMember("test-creator-email-4"))
        ).copy(
          teamId = Some("team-id"),
          deleted = Some(Deleted(LocalDateTime.now, "test-deleter-email-4"))
        )
        val applications: Seq[Application] = Seq(
          migratedApplication,
          deletedApplication,
          nonMigratedApplication,
          migratedDeletedApplication,
        )
        val summary: Seq[MigrationSummary] = Seq(
          MigrationSummary(migrated = true, deleted = false, count = 1),
          MigrationSummary(migrated = false, deleted = true, count = 1),
          MigrationSummary(migrated = false, deleted = false, count = 1),
          MigrationSummary(migrated = true, deleted = true, count = 1),
        )
        val teamApplications = TeamApplications(applications)

        running(fixture.playApplication) {
          implicit val msgs: Messages = messages(fixture.playApplication)
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
            GET,
            controllers.admin.routes.TeamMigrationController.onPageLoad().url
          )
          when(fixture.apiHubService.getApplications(same(None), same(true))(any))
            .thenReturn(Future.successful(applications))
          val result = route(fixture.playApplication, request).value
          val viewApplications = Seq(
            deletedApplication,
            nonMigratedApplication,
          )
          val view = fixture.playApplication.injector.instanceOf[TeamMigrationView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(viewApplications, summary, teamApplications, user).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }
    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
            GET,
            controllers.admin.routes.TeamMigrationController.onPageLoad().url
          )
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }
}
