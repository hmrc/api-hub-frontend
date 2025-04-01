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

package controllers.admin.addegresstoteam

import base.SpecBase
import controllers.actions.{FakeSupporter, FakeUser}
import controllers.routes
import models.UserAnswers
import models.team.Team
import models.user.UserModel
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.admin.addegresstoteam.AddEgressToTeamTeamPage
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AddEgressToTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.concurrent.Future

class AddEgressToTeamStartControllerSpec extends SpecBase with MockitoSugar {
  import AddEgressToTeamStartControllerSpec._

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val team = Team("teamId", "teamName", LocalDateTime.now(), Seq.empty)

  "AddEgressToTeamStartController" - {
    "must initiate user answers with current UserModel and Team, and persist this in the session repository for a support user" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
      when(fixture.apiHubService.findTeamById(any())(any())).thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.AddEgressToTeamStartController.onPageLoad(team.id))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeSupporter.userId, lastUpdated = clock.instant()).set(AddEgressToTeamTeamPage, team).success.value

        verify(fixture.sessionRepository).set(eqTo(expected))
      }
    }

    "must return Not Found if team id does not match any teams" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.findTeamById(any())(any())).thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.AddEgressToTeamStartController.onPageLoad(team.id))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        verify(fixture.sessionRepository, never()).set(any())
      }
    }

    "must redirect to the unauthorised page for a user who is not a support user" in {
      val fixture = buildFixture(FakeUser)

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.AddEgressToTeamStartController.onPageLoad(team.id))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)

        verify(fixture.sessionRepository, never()).set(any())
        verify(fixture.apiHubService, never()).findTeamById(any())(any())
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
      when(fixture.apiHubService.findTeamById(any())(any())).thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.AddEgressToTeamStartController.onPageLoad(team.id))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }

  }

  private case class Fixture(
    application: Application,
    apiHubService: ApiHubService,
    sessionRepository: AddEgressToTeamSessionRepository
  )

  private def buildFixture(user: UserModel): Fixture = {
    val sessionRepository = mock[AddEgressToTeamSessionRepository]
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), user)
      .overrides(
        bind[AddEgressToTeamSessionRepository].toInstance(sessionRepository),
        bind[ApiHubService].toInstance(apiHubService),
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock),
      )
      .build()

    Fixture(application, apiHubService, sessionRepository)
  }

}

object AddEgressToTeamStartControllerSpec {

  private val nextPage = controllers.routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

}
