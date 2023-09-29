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
import forms.AddAnApiSelectEndpointsFormProvider
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.{AvailableEndpoints, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddAnApiApiIdPage, AddAnApiSelectEndpointsPage}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import views.html.AddAnApiSelectEndpointsView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiSelectEndpointsControllerSpec extends SpecBase with MockitoSugar with ApiDetailGenerators {

  private def nextPage = Call("GET", "/foo")
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private lazy val addAnApiSelectEndpointsRoute = routes.AddAnApiSelectEndpointsController.onPageLoad(NormalMode).url

  private val apiDetail = sampleApiDetail()
  private val formProvider = new AddAnApiSelectEndpointsFormProvider()
  private val form = formProvider(apiDetail)

  "AddAnApiSelectEndpoints Controller" - {

    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(GET, addAnApiSelectEndpointsRoute)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[AddAnApiSelectEndpointsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, Some(FakeUser), apiDetail)(request, messages(fixture.application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = buildUserAnswers(apiDetail)
        .set(AddAnApiSelectEndpointsPage, AvailableEndpoints(apiDetail).keySet).success.value

      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request = FakeRequest(GET, addAnApiSelectEndpointsRoute)

        val view = fixture.application.injector.instanceOf[AddAnApiSelectEndpointsView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(AvailableEndpoints(apiDetail).keySet), NormalMode, Some(FakeUser), apiDetail)(request, messages(fixture.application)).toString
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
            .withFormUrlEncodedBody(("value[0]", AvailableEndpoints(apiDetail).keySet.head.toString()))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual nextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.application) {
        val request =
          FakeRequest(POST, addAnApiSelectEndpointsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = fixture.application.injector.instanceOf[AddAnApiSelectEndpointsView]

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Some(FakeUser), apiDetail)(request, messages(fixture.application)).toString
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
            .withFormUrlEncodedBody(("value[0]", AvailableEndpoints(apiDetail).keySet.head.toString()))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock)
      )
      .build()

    Fixture(application, apiHubService, addAnApiSessionRepository)
  }

  private def buildUserAnswers(apiDetail: ApiDetail): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(AddAnApiApiIdPage, apiDetail.id).toOption.value
  }

}
