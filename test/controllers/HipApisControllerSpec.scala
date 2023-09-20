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

import base.OptionallyAuthenticatedSpecBase
import controllers.actions.FakeUser
import forms.SearchHipApisFormProvider
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation
import views.html.HipApisView

import scala.concurrent.Future

class HipApisControllerSpec
  extends OptionallyAuthenticatedSpecBase
    with MockitoSugar
    with ScalaCheckDrivenPropertyChecks
    with ApiDetailGenerators
    with HtmlValidation {

  "GET" - {
    "must return OK and the correct view when the API detail exists for an unauthenticated user" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[HipApisView]

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getAllHipApis()(any()))
            .thenReturn(Future.successful(Seq(apiDetail)))

          val request = FakeRequest(GET, routes.HipApisController.onPageLoad().url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(None, Seq(apiDetail))(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[HipApisView]

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getAllHipApis()(any()))
            .thenReturn(Future.successful(Seq(apiDetail)))

          val request = FakeRequest(GET, routes.HipApisController.onPageLoad().url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(Some(FakeUser), Seq(apiDetail))(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
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
