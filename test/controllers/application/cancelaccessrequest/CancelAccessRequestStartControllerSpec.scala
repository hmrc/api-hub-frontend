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

package controllers.application.cancelaccessrequest

import base.SpecBase
import controllers.actions.{FakeApplication, FakeApplicationAuthActions}
import generators.AccessRequestGenerator
import models.accessrequest.{AccessRequest, Pending}
import models.user.UserModel
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import pages.application.cancelaccessrequest.{CancelAccessRequestApplicationPage, CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage, CancelAccessRequestStartPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.CancelAccessRequestSessionRepository
import services.ApiHubService
import utils.{TestHelpers, UserAnswersSugar}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class CancelAccessRequestStartControllerSpec extends SpecBase with MockitoSugar with TestHelpers with AccessRequestGenerator with FakeApplicationAuthActions with UserAnswersSugar {

  import CancelAccessRequestStartControllerSpec.*

  "CancelAccessRequestStartControllerSpec.startJourney" - {
    "must initiate user answers and persist this in the session repository" in {
      forAll(teamMemberAndSupporterTable) {(user: UserModel) =>
        val fixture = buildFixture(user)
        val userAnswers = buildUserAnswers(user, fixture)

        running(fixture.playApplication) {
          val request = FakeRequest(routes.CancelAccessRequestStartController.startJourney(FakeApplication.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          verify(fixture.apiHubService).getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(Some(Pending)))(any)
          verify(fixture.sessionRepository).set(eqTo(userAnswers))
        }
      }
    }

    "must redirect to the first page in the journey" in {
      forAll(teamMemberAndSupporterTable) {(user: UserModel) =>
        val fixture = buildFixture(user)
        val userAnswers = buildUserAnswers(user, fixture)

        running(fixture.playApplication) {
          val request = FakeRequest(routes.CancelAccessRequestStartController.startJourney(FakeApplication.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe onwardRoute.url
          verify(fixture.navigator).nextPage(eqTo(CancelAccessRequestStartPage), eqTo(NormalMode), eqTo(userAnswers))
        }
      }
    }

    "must redirect to unauthorised if the user is not a team member or support" in {
      forAll(nonTeamMembersOrSupport) {(user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(routes.CancelAccessRequestStartController.startJourney(FakeApplication.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
          verifyNoInteractions(fixture.sessionRepository)
        }
      }
    }
  }

  "CancelAccessRequestStartControllerSpec.startJourneyWithAccessRequest" - {
    "must initiate user answers and persist this in the session repository" in {
      forAll(teamMemberAndSupporterTable) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val accessRequestId = fixture.accessRequests.head.id
        val userAnswers = buildUserAnswers(user, fixture, Some(Set(accessRequestId)))

        running(fixture.playApplication) {
          val request = FakeRequest(routes.CancelAccessRequestStartController.startJourneyWithAccessRequest(FakeApplication.id, accessRequestId))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          verify(fixture.apiHubService).getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(Some(Pending)))(any)
          verify(fixture.sessionRepository).set(eqTo(userAnswers))
        }
      }
    }

    "must redirect to the first page in the journey" in {
      forAll(teamMemberAndSupporterTable) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val accessRequestId = fixture.accessRequests.head.id
        val userAnswers = buildUserAnswers(user, fixture, Some(Set(accessRequestId)))

        running(fixture.playApplication) {
          val request = FakeRequest(routes.CancelAccessRequestStartController.startJourneyWithAccessRequest(FakeApplication.id, accessRequestId))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe onwardRoute.url
          verify(fixture.navigator).nextPage(eqTo(CancelAccessRequestStartPage), eqTo(NormalMode), eqTo(userAnswers))
        }
      }
    }

    "must redirect to unauthorised if the user is not a team member or support" in {
      forAll(nonTeamMembersOrSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(routes.CancelAccessRequestStartController.startJourneyWithAccessRequest(FakeApplication.id, "an access request id"))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
          verifyNoInteractions(fixture.sessionRepository)
        }
      }
    }
  }

  private def buildFixture(user: UserModel): Fixture = {
    val sessionRepository = mock[CancelAccessRequestSessionRepository]
    val apiHubService = mock[ApiHubService]
    val navigator = mock[Navigator]
    val accessRequests = sampleAccessRequests()

    when(sessionRepository.set(any)).thenReturn(Future.successful(true))
    when(apiHubService.getApplication(any, any)(any)).thenReturn(Future.successful(Some(FakeApplication)))
    when(apiHubService.getAccessRequests(any, any)(any)).thenReturn(Future.successful(accessRequests))
    when(navigator.nextPage(any, any, any)).thenReturn(onwardRoute)

    val playApplication = applicationBuilder(user = user)
      .overrides(
        bind[Clock].toInstance(clock),
        bind[CancelAccessRequestSessionRepository].toInstance(sessionRepository),
        bind[ApiHubService].toInstance(apiHubService),
        bind[Navigator].toInstance(navigator)
      )
      .build()

    Fixture(playApplication, sessionRepository, apiHubService, navigator, accessRequests)
  }

}

object CancelAccessRequestStartControllerSpec extends OptionValues {

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val onwardRoute = controllers.routes.IndexController.onPageLoad

  private case class Fixture(
    playApplication: PlayApplication,
    sessionRepository: CancelAccessRequestSessionRepository,
    apiHubService: ApiHubService,
    navigator: Navigator,
    accessRequests: Seq[AccessRequest]
  )

  private def buildUserAnswers(user: UserModel, fixture: Fixture, maybeSelectedRequests: Option[Set[String]] = None): UserAnswers = {

    val userAnswers = UserAnswers(id = user.userId, lastUpdated = clock.instant())
      .set(CancelAccessRequestApplicationPage, FakeApplication).toOption.value
      .set(CancelAccessRequestPendingPage, fixture.accessRequests).toOption.value

    if (maybeSelectedRequests.isDefined) {
      userAnswers.set(CancelAccessRequestSelectApiPage, maybeSelectedRequests.get).toOption.value
    } else {
      userAnswers
    }

  }

}
