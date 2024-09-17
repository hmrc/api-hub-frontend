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

package controllers.myapis.produce

import base.SpecBase
import controllers.actions.FakeUser
import models.UserAnswers
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ProduceApiStartControllerSpec extends SpecBase with MockitoSugar {

  import ProduceApiStartControllerSpec.*

  "RegisterApplicationStartController" - {
    "must initiate user answers and persist this in the session repository" in {
      val fixture = buildFixture()
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.produce.routes.ProduceApiStartController.startProduceApi())
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())

        verify(fixture.sessionRepository).set(eqTo(expected))
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture()
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.produce.routes.ProduceApiStartController.startProduceApi())
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }

  }

  private case class Fixture(
    application: Application,
    sessionRepository: ProduceApiSessionRepository
  )

  private def buildFixture(userEmail: String = FakeUser.email): Fixture = {
    val sessionRepository = mock[ProduceApiSessionRepository]

    val application = applicationBuilder(user = FakeUser.copy(email = userEmail))
      .overrides(
        bind[ProduceApiSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock),
      )
      .build()

    Fixture(application, sessionRepository)
  }

}

object ProduceApiStartControllerSpec {

  private val nextPage = controllers.routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

}
