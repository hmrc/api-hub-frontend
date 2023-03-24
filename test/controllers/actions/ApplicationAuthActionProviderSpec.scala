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

package controllers.actions

import base.SpecBase
import config.InternalAuthTokenInitialiser
import controllers.ApproveScopeControllerSpec.{Fixture, applicationBuilder, mock}
import controllers.routes
import models.requests.{ApplicationRequest, IdentifierRequest}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationAuthActionProviderSpec extends SpecBase with MockitoSugar {

  "ApplicationAuthActionProvider" - {
    "must blah blah" in {
      val fixture = buildFixture(FakeApprover)
      val testId = "test-app-id"

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(testId))(any())) thenReturn Future.successful(Some(FakeApplication))

      running(fixture.application) {
        val provider = fixture.application.injector.instanceOf[ApplicationAuthActionProvider]
        val action = provider.apply(testId)
        action mustBe "hello"

      }

    }
  }

  def buildFixture(user: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = None, user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}
