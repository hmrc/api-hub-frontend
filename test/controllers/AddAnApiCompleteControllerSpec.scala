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
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live, Maintainer}
import models.{AddAnApi, ApiPolicyConditionsDeclaration, AvailableEndpoint, CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddAnApiApiPage, AddAnApiContextPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage, ApiPolicyConditionsDeclarationPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.HtmlValidation
import views.html.ErrorTemplate

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiCompleteControllerSpec extends SpecBase with HtmlValidation with MockitoSugar {

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val selectedScopes = Set(Set("test-scope-1", "test-scope-2"))
  private val acceptedPolicyConditions: Set[ApiPolicyConditionsDeclaration] = Set(ApiPolicyConditionsDeclaration.Accept)
  private val apiId = "test-api-id"
  private val apiTitle = "test-api-title"
  private val expectedScopes = Set("test-scope-1", "test-scope-2")
  private val apiDetail = ApiDetail(apiId,
    "publisher ref",
    apiTitle,
    "",
    "",
    Seq(Endpoint("/foo/bar", Seq(EndpointMethod("GET", None, None, expectedScopes.toSeq)))),
    None,
    "",
    Live,
    reviewedDate = Instant.now(),
    platform = "API_PLATFORM",
    maintainer = Maintainer("name", "#slack", List.empty)
  )

  private val fullUserAnswers = emptyUserAnswers
    .set(AddAnApiContextPage, AddAnApi).toOption.value
    .set(AddAnApiApiPage, apiDetail).toOption.value
    .set(AddAnApiSelectApplicationPage, FakeApplication).toOption.value
    .set(AddAnApiSelectEndpointsPage, selectedScopes).toOption.value
    .set(ApiPolicyConditionsDeclarationPage, acceptedPolicyConditions).toOption.value

  "AddAnApiCompleteController" - {
    "must place the correct request when a valid set of answers is submitted and redirect to the success page" in {

      val fixture = buildFixture(Some(fullUserAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.addApi(eqTo(FakeApplication.id),
        eqTo(apiId),
        eqTo(apiTitle),
        eqTo(
            Seq(
              AvailableEndpoint(
                "/foo/bar",
                EndpointMethod("GET", None, None, expectedScopes.toSeq),
                false
              )
            )
        )
      )(any()))
        .thenReturn(Future.successful(Some(())))

      when(fixture.addAnApiSessionRepository.clear(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.AddAnApiSuccessController.onPageLoad(FakeApplication.id, apiId).url)
      }
    }

    "must delete the user answers on success" in {
      val fixture = buildFixture(Some(fullUserAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.addApi(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(())))

      when(fixture.addAnApiSessionRepository.clear(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        verify(fixture.addAnApiSessionRepository).clear(eqTo(FakeUser.userId))
      }
    }

    "must return a Not Found page when the application does not exist" in {
      val fixture = buildFixture(Some(fullUserAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.addApi(eqTo(FakeApplication.id), eqTo(apiId), any(), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with ID ${FakeApplication.id}."
          )(request, messages(fixture.application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the journey recovery page when no API Id answer exists" in {
      val userAnswers = fullUserAnswers.remove(AddAnApiApiPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "must redirect to the select application page in check mode page when the selected application answer is missing" in {
      val userAnswers = fullUserAnswers.remove(AddAnApiSelectApplicationPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.AddAnApiSelectApplicationController.onPageLoad(CheckMode).url)
      }
    }

    "must redirect to the select endpoints page in check mode page when the selected endpoints answer is missing" in {
      val userAnswers = fullUserAnswers.remove(AddAnApiSelectEndpointsPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode, AddAnApi).url)
      }
    }

    "must redirect to the accept policy declaration page in check mode page when the policy declaration answer is missing" in {
      val userAnswers = fullUserAnswers.remove(ApiPolicyConditionsDeclarationPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(CheckMode, AddAnApi).url)
      }
    }

    "must redirect to Journey Recovery if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "must return a 500 Internal Server Error with suitable message if the backend returned 502 Bad Gateway" in {
      val fixture = buildFixture(Some(fullUserAnswers))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any())).thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.addApi(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("test-message", BAD_GATEWAY)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi(AddAnApi).url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe view.apply(
          pageTitle = "Sorry, there is a problem with the service - 500",
          heading = "Sorry, there is a problem with the service",
          message = "You should check this application's details before trying again as it is possible that this action was partially successful."
        )(request, messages(fixture.application))
          .toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(
                              application: Application,
                              apiHubService: ApiHubService,
                              addAnApiSessionRepository: AddAnApiSessionRepository
                            )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val addAnApiSessionRepository = mock[AddAnApiSessionRepository]

    val application = applicationBuilder(userAnswers)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[AddAnApiSessionRepository].toInstance(addAnApiSessionRepository),
        bind[Clock].toInstance(clock)
      )
      .build()

    Fixture(application, apiHubService, addAnApiSessionRepository)
  }

}
