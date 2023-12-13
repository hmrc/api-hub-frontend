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
import models.{AddAnApi, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddAnApiApiPage, AddAnApiContextPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiStartControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  private val nextPage = routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "AddAnApiStartController" - {
    "must initiate user answers with the Api Id and persist this in the session repository" in {
      val fixture = buildFixture()
      val apiDetail = sampleApiDetail()

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.addAnApiSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiStartController.addAnApi(apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
          .set(AddAnApiApiPage, apiDetail)
          .flatMap(_.set(AddAnApiContextPage, AddAnApi))
          .toOption.value

        verify(fixture.addAnApiSessionRepository).set(ArgumentMatchers.eq(expected))
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture()
      val apiDetail = sampleApiDetail()

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.addAnApiSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiStartController.addAnApi(apiDetail.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }

    "must return 404 Not Found if the Api does not exist" in {
      val fixture = buildFixture()
      val apiDetail = sampleApiDetail()

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiStartController.addAnApi(apiDetail.id).url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id ${apiDetail.id}."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(
    application: Application,
    apiHubService: ApiHubService,
    addAnApiSessionRepository: AddAnApiSessionRepository,
    addAnApiStartController: AddAnApiStartController
  )

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]
    val addAnApiSessionRepository = mock[AddAnApiSessionRepository]

    val application = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[AddAnApiSessionRepository].toInstance(addAnApiSessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock)
      )
      .build()

    val controller = application.injector.instanceOf[AddAnApiStartController]
    Fixture(application, apiHubService, addAnApiSessionRepository, controller)
  }

}
