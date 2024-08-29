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

package controllers

import base.SpecBase
import generators.ApiDetailGenerators
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

class ApiSupportControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with ApiDetailGenerators {

  "onPageLoad" - {
    "must return the correct ApiDetail as JSON to a user with the support role" in {
      val apiDetail = sampleApiDetail()

      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.routes.ApiSupportController.onPageLoad(apiDetail.id))
          val result = route(fixture.playApplication, request).value

          val expected = apiDetail.copy(
            openApiSpecification = controllers.routes.OasRedocController.getOas(apiDetail.id).absoluteURL()(request)
          )

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(expected)
        }
      }
    }

    "must redirect to the unauthorised page for a user who is not support" in {
      forAll(usersWhoCannotSupport) {(user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.routes.ApiSupportController.onPageLoad("test-id"))
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
