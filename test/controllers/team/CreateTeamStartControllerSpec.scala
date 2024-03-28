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

package controllers.team

import base.SpecBase
import controllers.actions.FakeUser
import generators.ApiDetailGenerators
import models.UserAnswers
import models.application.TeamMember
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.CreateTeamStartPage
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.CreateTeamSessionRepository
import utils.HtmlValidation
import views.html.ErrorTemplate

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class CreateTeamStartControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  private val nextPage = controllers.routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "CreateTeamStartController" - {
    "must initiate user answers with single team member and persist this in the session repository" in {
      val fixture = buildFixture()
      when(fixture.createTeamSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.CreateTeamStartController.startCreateTeam().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
          .set(CreateTeamStartPage, Seq[TeamMember](TeamMember(FakeUser.email.get)))
          .toOption.value

        verify(fixture.createTeamSessionRepository).set(ArgumentMatchers.eq(expected))
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture()
      when(fixture.createTeamSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.CreateTeamStartController.startCreateTeam().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }

    "must return an error if user has no email address" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, routes.CreateTeamStartController.startCreateTeam().url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe
          view(
            "Sorry, we are experiencing technical difficulties - 500",
            "Sorry, we’re experiencing technical difficulties",
            "Please try again in a few minutes."
          )(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(
    application: Application,
    createTeamSessionRepository: CreateTeamSessionRepository,
    createTeamStartController: CreateTeamStartController
  )

  private def buildFixture(userEmail: Option[String] = FakeUser.email): Fixture = {
    val createTeamSessionRepository = mock[CreateTeamSessionRepository]

    val application = applicationBuilder(user = FakeUser.copy(email = userEmail))
      .overrides(
        bind[CreateTeamSessionRepository].toInstance(createTeamSessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock),
      )
      .build()

    val controller = application.injector.instanceOf[CreateTeamStartController]
    Fixture(application, createTeamSessionRepository, controller)
  }

}
