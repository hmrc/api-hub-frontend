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

import controllers.actions.{FakeOptionalIdentifierAction, FakeUser, OptionalIdentifierAction, OptionalUserProvider, OptionalUserProviderImpl}
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.{ApiDetailsView, ErrorTemplate}

import scala.concurrent.Future

class ApiDetailsControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar with ScalaCheckDrivenPropertyChecks with ApiDetailGenerators {

  "GET" - {
    "must return OK and the correct view when the API detail exists for an unauthenticated user" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, None)(request, messages(fixture.application)).toString()
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, Some(FakeUser))(request, messages(fixture.application)).toString()
        }
      }
    }

    "must return a 404 Not Found page when the API detail does not exist" in {
      val fixture = buildFixture()
      val id = "test-id"

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(id))(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id $id."
          )(request, messages(fixture.application))
            .toString()
      }
    }
  }

  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: Option[UserModel] = None): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = GuiceApplicationBuilder()
      .overrides(
        bind[OptionalUserProvider].toInstance(new OptionalUserProviderImpl(userModel)),
        bind[OptionalIdentifierAction].to[FakeOptionalIdentifierAction],
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(apiHubService, application)
  }

  def messages(application: Application): Messages = {
    application.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  }

}
