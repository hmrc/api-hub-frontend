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

package controllers

import base.SpecBase
import controllers.RequestScopeSuccessControllerSpec.{applicationId, buildFixture}
import controllers.actions.FakeUser
import models.application.{Application, Creator}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.{Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.RequestScopeSuccessView

import scala.concurrent.Future

class RequestScopeSuccessControllerSpec extends SpecBase with MockitoSugar {

  "RequestScopeSuccess Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture()
      val application = Application(applicationId, "test-name", Creator("test-email"))

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id))(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.RequestScopeSuccessController.onPageLoad(applicationId).url)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[RequestScopeSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(application, Some(FakeUser))(request, messages(fixture.playApplication)).toString
      }

    }

    "must return 404 Not Found if the application does not exist" in {

      val fixture = buildFixture()
      val application = Application(applicationId, "test-name", Creator("test-email"))

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.RequestScopeSuccessController.onPageLoad(applicationId).url)

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual NOT_FOUND
      }

    }

  }

}

object RequestScopeSuccessControllerSpec extends SpecBase with MockitoSugar {

  val applicationId: String = "test-id"

  case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  def buildFixture(): Fixture = {
    val apiHubService =  mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(playApplication, apiHubService)
  }

}
