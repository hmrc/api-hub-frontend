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

package controllers.application

import base.SpecBase
import controllers.routes
import generators.ApplicationGenerator
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.TestHelpers

import scala.concurrent.Future

class ApplicationSupportControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with ApplicationGenerator {

  "onPageLoad" - {
    "must return the correct Application as JSON to a user with the support role" in {
      val application = sampleApplication()

      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(true))(any))
          .thenReturn(Future.successful(Some(application)))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.ApplicationSupportController.onPageLoad(application.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(application)
        }
      }
    }

    "must redirect to the unauthorised page for a user who is not support" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.ApplicationSupportController.onPageLoad("test-id"))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
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
