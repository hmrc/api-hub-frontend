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
import models.application.{Application, Creator}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import views.html.RegisterApplicationSuccessView

import scala.concurrent.Future

class RegisterApplicationSuccessControllerSpec extends SpecBase with MockitoSugar{

  "RegisterApplicationSuccess Controller" - {

    "must return OK and the correct view for a GET" in {
      val fixture = RegisterApplicationSuccessControllerSpec.buildFixture()
      val app = Application("id-1", "test", Creator("creator-email"))

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq("id-1"))(any()))
        .thenReturn(Future.successful(Some(app)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.RegisterApplicationSuccessController.onPageLoad("id-1").url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[RegisterApplicationSuccessView]

        status(result) mustEqual OK

        val expected = view(app)(request, messages(fixture.application)).toString
        val actual = contentAsString(result)
        actual mustEqual expected
      }
    }
  }
}
object RegisterApplicationSuccessControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
                      application: PlayApplication,
                      apiHubService: ApiHubService
                    )

  def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userAnswers = None)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

}