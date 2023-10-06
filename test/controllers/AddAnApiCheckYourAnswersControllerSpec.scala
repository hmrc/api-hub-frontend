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
import controllers.actions.{FakeApplication, FakeUser}
import models.UserAnswers
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.checkAnswers.{AddAnApiApiIdSummary, AddAnApiSelectApplicationSummary, AddAnApiSelectEndpointsSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.AddAnApiCheckYourAnswersView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiCheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with HtmlValidation {

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "AddAnApiCheckYourAnswersController" - {
    "must return OK and the correct view for a GET with empty user answers" in {
      val fixture = buildFixture(Some(emptyUserAnswers))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[AddAnApiCheckYourAnswersView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(SummaryListViewModel(Seq.empty), Some(FakeUser))(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET with complete user answers" in {
      val apiDetail = ApiDetail(
        "test-id",
        "test-title",
        "test-description",
        "test-version",
        Seq(
          Endpoint(
            "/path1",
            Seq(
              EndpointMethod("GET", None, None, Seq("test-scope-1"))
            )
          )
        ),
        None
      )

      val userAnswers = emptyUserAnswers
        .set(AddAnApiApiIdPage, apiDetail.id).toOption.value
        .set(AddAnApiSelectApplicationPage, FakeApplication.id).toOption.value
        .set(AddAnApiSelectEndpointsPage, Set(Set("test-scope-1"))).toOption.value

      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[AddAnApiCheckYourAnswersView]

        val summaryList = SummaryListViewModel(
          Seq(
            AddAnApiSelectApplicationSummary.row(Some(FakeApplication))(messages(fixture.application)).value,
            AddAnApiApiIdSummary.row(Some(apiDetail))(messages(fixture.application)).value,
            AddAnApiSelectEndpointsSummary.row(userAnswers, apiDetail)(messages(fixture.application)).value
          )
        )

        status(result) mustBe OK
        contentAsString(result) mustBe view(summaryList, Some(FakeUser))(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    application: Application,
    apiHubService: ApiHubService
  )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val addAnApiSessionRepository = mock[AddAnApiSessionRepository]

    val application = applicationBuilder(userAnswers)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[Clock].toInstance(clock)
      )
      .build()

    Fixture(application, apiHubService)
  }

}
