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
import forms.AddAnApiSelectEndpointsFormProvider
import generators.ApiDetailGenerators
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import models.application.{Api, Application, SelectedEndpoint}
import models.{AddAnApi, AvailableEndpoints, NormalMode, UserAnswers}
import models.application.ApplicationLenses._
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddAnApiApiPage, AddAnApiContextPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage}
import play.api.{Application => PlayApplication}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.AddAnApiSelectEndpointsView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiSelectEndpointsControllerSpec extends SpecBase with MockitoSugar with ApiDetailGenerators with HtmlValidation {

  private def nextPage = Call("GET", "/foo")
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private lazy val addAnApiSelectEndpointsRoute = routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode, AddAnApi).url

  private val apiDetail = sampleApiDetail()
  private val formProvider = new AddAnApiSelectEndpointsFormProvider()
  private val form = formProvider(apiDetail, FakeApplication)

  "AddAnApiSelectEndpoints Controller" - {

    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, addAnApiSelectEndpointsRoute)
        implicit val msgs: Messages = messages(fixture.application)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[AddAnApiSelectEndpointsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, AddAnApi, Some(FakeUser), apiDetail, FakeApplication).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = buildUserAnswers(apiDetail)
        .set(AddAnApiSelectEndpointsPage, AvailableEndpoints(apiDetail, FakeApplication).keySet).success.value

      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, addAnApiSelectEndpointsRoute)
        implicit val msgs: Messages = messages(fixture.application)
        val view = fixture.application.injector.instanceOf[AddAnApiSelectEndpointsView]
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(AvailableEndpoints(apiDetail, FakeApplication).keySet), NormalMode, AddAnApi, Some(FakeUser), apiDetail, FakeApplication).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.addAnApiSessionRepository.set(any())) thenReturn Future.successful(true)

      running(fixture.application) {
        val request =
          FakeRequest(POST, addAnApiSelectEndpointsRoute)
            .withFormUrlEncodedBody(("value[0]", AvailableEndpoints(apiDetail, FakeApplication).keySet.head.toString()))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual nextPage.url
      }
    }

    "must save the answer when valid data is submitted" in {
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.addAnApiSessionRepository.set(any())) thenReturn Future.successful(true)

      running(fixture.application) {
        val request =
          FakeRequest(POST, addAnApiSelectEndpointsRoute)
            .withFormUrlEncodedBody(("value[0]", AvailableEndpoints(apiDetail, FakeApplication).keySet.head.toString()))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER

        val expected = buildUserAnswers(apiDetail)
          .set(AddAnApiSelectEndpointsPage, Set(AvailableEndpoints(apiDetail, FakeApplication).keySet.head)).toOption.value

        verify(fixture.addAnApiSessionRepository).set(ArgumentMatchers.eq(expected))
      }
    }

    "must add further endpoints to an existing answer (add endpoints journey)" in {
      val apiDetail = sampleApiDetail()
        .copy(
          endpoints = Seq(
            Endpoint("/test-path-1", Seq(EndpointMethod("GET", None, None, Seq("test-scope-1")))),
            Endpoint("/test-path-2", Seq(EndpointMethod("GET", None, None, Seq("test-scope-2")))),
            Endpoint("/test-path-3", Seq(EndpointMethod("GET", None, None, Seq("test-scope-3"))))
          )
        )

      val application = FakeApplication
        .addApi(
          Api(apiDetail.id, Seq(SelectedEndpoint("GET", "/test-path-2")))
        )

      val fixture = buildFixture(Some(buildUserAnswers(apiDetail, application)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.addAnApiSessionRepository.set(any())) thenReturn Future.successful(true)

      running(fixture.application) {
        val request =
          FakeRequest(POST, addAnApiSelectEndpointsRoute)
            .withFormUrlEncodedBody(("value[0]", Set("test-scope-1").toString()))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER

        val expected = buildUserAnswers(apiDetail, application)
          .set(AddAnApiSelectEndpointsPage, Set(Set("test-scope-1"), Set("test-scope-2"))).toOption.value

        verify(fixture.addAnApiSessionRepository).set(ArgumentMatchers.eq(expected))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, addAnApiSelectEndpointsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))
        implicit val msgs: Messages = messages(fixture.application)

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = fixture.application.injector.instanceOf[AddAnApiSelectEndpointsView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, AddAnApi, Some(FakeUser), apiDetail, FakeApplication).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, addAnApiSelectEndpointsRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request =
          FakeRequest(POST, addAnApiSelectEndpointsRoute)
            .withFormUrlEncodedBody(("value[0]", "test-value"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    application: PlayApplication,
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
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock)
      )
      .build()

    Fixture(application, apiHubService, addAnApiSessionRepository)
  }

  private def buildUserAnswers(apiDetail: ApiDetail, application: Application = FakeApplication): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(AddAnApiApiPage, apiDetail).toOption.value
      .set(AddAnApiContextPage, AddAnApi).toOption.value
      .set(AddAnApiSelectApplicationPage, application).toOption.value
  }

}
