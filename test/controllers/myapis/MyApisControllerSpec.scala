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

package controllers.myapis

import base.OptionallyAuthenticatedSpecBase
import controllers.actions.FakeUser
import controllers.routes
import fakes.{FakeDomains, FakeHods}
import generators.ApiDetailGenerators
import models.api.{ApiDetail, Live}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.{AsyncTestSuite, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation
import views.html.HipApisView
import views.html.myapis.MyApisView

import scala.concurrent.Future

class MyApisControllerSpec
  extends OptionallyAuthenticatedSpecBase
    with MockitoSugar
    with ScalaCheckDrivenPropertyChecks
    with ApiDetailGenerators
    with HtmlValidation
    with OptionValues
    with AsyncTestSuite {

  "GET" - {
    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[MyApisView]

        forAll { (apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getUserApis(any())(any, any))(any)
            .thenReturn(Future.successful(Seq(apiDetail)))

          val request = FakeRequest(GET, controllers.myapis.routes.MyApisController.onPageLoad().url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(Seq(apiDetail), FakeUser)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the authenticated user has no teams or no apis" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        when(fixture.apiHubService.getUserApis(any())(any, any))(any)
          .thenReturn(Future.successful(Seq.empty))

        val request = FakeRequest(GET, controllers.myapis.routes.MyApisController.onPageLoad().url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }


  "must return OK and the correct view when the API detail exists for an unauthenticated user" in {
    val fixture = buildFixture(userModel = None)

    running(fixture.application) {
      val request = FakeRequest(GET, controllers.myapis.routes.MyApisController.onPageLoad().url)
      val result = route(fixture.application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }
  }

  "must return the apis in case-insensitive alphabetical order" in {
    val fixture = buildFixture()

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[HipApisView]

      val zebras = ApiDetail("id1", "ref1", "zebras", "zebras api", "1.0.0", Seq.empty, None, "oas", Live)
      val molluscs = ApiDetail("id2", "ref2", "MOLLUSCS", "molluscs api", "1.0.0", Seq.empty, None, "oas", Live)
      val aardvarks = ApiDetail("id3", "ref3", "aardvarks", "aardvarks api", "1.0.0", Seq.empty, None, "oas", Live)
      val pigeons = ApiDetail("id4", "ref4", "PIGEONS", "pigeons api", "1.0.0", Seq.empty, None, "oas", Live)

      when(fixture.apiHubService.getUserApis(any)(any, any))
        .thenReturn(Future.successful(Seq(molluscs, zebras, aardvarks, pigeons)))

      val request = FakeRequest(GET, routes.HipApisController.onPageLoad().url)
      val result = route(fixture.application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(None, Seq(aardvarks, molluscs, pigeons, zebras), FakeDomains, FakeHods)(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }


  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: Option[UserModel] = None): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(apiHubService, application)
  }
}
