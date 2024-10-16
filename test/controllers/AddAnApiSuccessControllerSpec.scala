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
import controllers.actions.FakeUser
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.application.Application
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.{AddAnApiSuccessView, ErrorTemplate}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiSuccessControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  val apiDetail: ApiDetail = mock[ApiDetail]
  when(apiDetail.title).thenReturn("API title")
  when(apiDetail.id).thenReturn("api_id")

  val app: Application = mock[Application]
  when(app.id).thenReturn("app_id")
  when(app.name).thenReturn("app_name")

  private val nextPage = routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "GET" - {

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[AddAnApiSuccessView]

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))

        when(fixture.apiHubService.getApplication(eqTo(app.id), any(), any())(any()))
          .thenReturn(Future.successful(Some(app)))

        val request = FakeRequest(GET, routes.AddAnApiSuccessController.onPageLoad(app.id, apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(app, apiDetail, Some(FakeUser))(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }


    "must return a 404 Not Found page when the API detail does not exist" in {
      val fixture = buildFixture

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(None))

        when(fixture.apiHubService.getApplication(eqTo(app.id), any(), any())(any()))
          .thenReturn(Future.successful(Some(app)))

        val request = FakeRequest(GET, routes.AddAnApiSuccessController.onPageLoad(app.id, apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with ID ${apiDetail.id}."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must return a 404 Not Found page when the Application does not exist" in {
      val fixture = buildFixture

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))

        when(fixture.apiHubService.getApplication(eqTo(app.id), any(), any())(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.AddAnApiSuccessController.onPageLoad(app.id, apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with ID ${app.id}."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must return a 404 Not Found page when neither the Application not the Api exist" in {
      val fixture = buildFixture

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(None))

        when(fixture.apiHubService.getApplication(eqTo(app.id), any(), any())(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.AddAnApiSuccessController.onPageLoad(app.id, apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Neither API nor application found",
            s"Cannot find an API with ID ${apiDetail.id} or an application with ID ${app.id}."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              addAnApiSessionRepository: AddAnApiSessionRepository,
                              addAnApiSuccessController: AddAnApiSuccessController
                            )

  private def buildFixture: Fixture = {
    val apiHubService = mock[ApiHubService]
    val addAnApiSessionRepository = mock[AddAnApiSessionRepository]

    val application = applicationBuilder(None)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[AddAnApiSessionRepository].toInstance(addAnApiSessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock)
      )
      .build()

    val controller = application.injector.instanceOf[AddAnApiSuccessController]
    Fixture(application, apiHubService, addAnApiSessionRepository, controller)
  }

}