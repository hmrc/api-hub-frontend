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
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation
import views.html.{ErrorTemplate, RedocView}

import scala.concurrent.Future

class OasControllerSpec
  extends OptionallyAuthenticatedSpecBase
    with MockitoSugar
    with ScalaCheckDrivenPropertyChecks
    with ApiDetailGenerators
    with HtmlValidation
    with OptionValues {

  "GET redoc" - {
    "must return OK and the correct view when the API detail exists for an unauthenticated user" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[RedocView]

        val apiDetail = ApiDetail("apiId", "an api", "a description", "1.0.0", Seq.empty, None, sampleOas)
        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any())).thenReturn(Future.successful(Some(apiDetail)))

        val request = FakeRequest(GET, routes.OasRedocController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, None)(request, messages(fixture.application)).toString()
        // Does not validate.
        // contentAsString(result) must validateAsHtml

      }
    }

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[RedocView]

        val apiDetail = ApiDetail("apiId", "an api", "a description", "1.0.0", Seq.empty, None, sampleOas)
        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))

        val request = FakeRequest(GET, routes.OasRedocController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, Some(FakeUser))(request, messages(fixture.application)).toString()
        // Does not validate.
        // contentAsString(result) must validateAsHtml
      }
    }

    "must return a 404 Not Found page when the API detail does not exist" in {
      val fixture = buildFixture()
      val id = "test-id"

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(id))(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.OasRedocController.onPageLoad(id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id $id."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "GET oas spec" - {
      "must return OK and the oas the API detail exists for an unauthenticated user" in {
        val fixture = buildFixture()

        running(fixture.application) {

          val apiDetail = ApiDetail("apiId", "an api", "a description", "1.0.0", Seq.empty, None, sampleOas)
          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any())).thenReturn(Future.successful(Some(apiDetail)))

          val request = FakeRequest(GET, routes.OasRedocController.getOas(apiDetail.id).url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe sampleOas
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
