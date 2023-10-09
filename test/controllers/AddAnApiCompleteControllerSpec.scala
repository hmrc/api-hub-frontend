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
import models.{ApiPolicyConditionsDeclaration, CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.MockitoSugar.mock
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage, ApiPolicyConditionsDeclarationPage}
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

class AddAnApiCompleteControllerSpec extends SpecBase with HtmlValidation {

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val selectedScopes = Set(Set("test-scope-1", "test-scope-2"), Set("test-scope-1", "test-scope-3"))
  private val acceptedPolicyConditions: Set[ApiPolicyConditionsDeclaration] = Set(ApiPolicyConditionsDeclaration.Accept)
  private val fullUserAnswers = emptyUserAnswers
    .set(AddAnApiApiIdPage, "test-api-id").toOption.value
    .set(AddAnApiSelectApplicationPage, FakeApplication.id).toOption.value
    .set(AddAnApiSelectEndpointsPage, selectedScopes).toOption.value
    .set(ApiPolicyConditionsDeclarationPage, acceptedPolicyConditions).toOption.value

  "AddAnApiCompleteController" - {
    "must place the correct request when a valid set of answers is submitted and redirect to the success page" in {
      val fixture = buildFixture(Some(fullUserAnswers))

      val expectedScopes = Set("test-scope-1", "test-scope-2", "test-scope-3")
      when(fixture.apiHubService.addScopes(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(expectedScopes))(any()))
        .thenReturn(Future.successful(Some(())))

      when(fixture.addAnApiSessionRepository.clear(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IndexController.onPageLoad.url)
      }
    }

    "must delete the user answers on success" in {
      val fixture = buildFixture(Some(fullUserAnswers))

      when(fixture.apiHubService.addScopes(any(), any())(any()))
        .thenReturn(Future.successful(Some(())))

      when(fixture.addAnApiSessionRepository.clear(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        verify(fixture.addAnApiSessionRepository).clear(ArgumentMatchers.eq(FakeUser.userId))
      }
    }

    "must return a Not Found page when the application does not exist" in {
      val fixture = buildFixture(Some(fullUserAnswers))

      when(fixture.apiHubService.addScopes(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(selectedScopes.flatten))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with Id ${FakeApplication.id}."
          )(request, messages(fixture.application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the journey recovery page when no API Id answer exists" in {
      val userAnswers = fullUserAnswers.remove(AddAnApiApiIdPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "must redirect to the select application page in check mode page when the selected application answer is missing" in {
      val userAnswers = fullUserAnswers.remove(AddAnApiSelectApplicationPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.AddAnApiSelectApplicationController.onPageLoad(CheckMode).url)
      }
    }

    "must redirect to the select endpoints page in check mode page when the selected endpoints answer is missing" in {
      val userAnswers = fullUserAnswers.remove(AddAnApiSelectEndpointsPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode).url)
      }
    }

    "must redirect to the accept policy declaration page in check mode page when the policy declaration answer is missing" in {
      val userAnswers = fullUserAnswers.remove(ApiPolicyConditionsDeclarationPage).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(CheckMode).url)
      }
    }

    "must redirect to Journey Recovery if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiCompleteController.addApi().url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
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
