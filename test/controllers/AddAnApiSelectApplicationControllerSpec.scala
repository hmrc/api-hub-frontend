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
import forms.AddAnApiSelectApplicationFormProvider
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Api, Application}
import models.{AddAnApi, Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddAnApiApiPage, AddAnApiContextPage, AddAnApiSelectApplicationPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.{AddAnApiSelectApplicationView, ErrorTemplate}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class AddAnApiSelectApplicationControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  private val nextPage = routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val form = new AddAnApiSelectApplicationFormProvider()()

  "AddAnApiSelectApplicationController" - {
    "must return OK and the correct view for a GET when the user has no applications" in {
      val apiDetail = sampleApiDetail()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(Seq.empty))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail, Seq.empty, Seq.empty)

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET when the user has applications with access" in {
      val apiDetail = sampleApiDetail()
      val application = buildApplicationWithAccess(apiDetail)
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(Seq(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail, Seq(application), Seq.empty)

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET when the user has less than 5 applications without access" in {
      val apiDetail = sampleApiDetail()
      val applications = buildApplicationsWithoutAccess(3)
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(applications))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail, Seq.empty, applications)

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET when the user has more than 5 applications without access" in {
      val apiDetail = sampleApiDetail()
      val applications = buildApplicationsWithoutAccess(10)
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(applications))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail, Seq.empty, applications)

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET when the user has applications both with and without access" in {
      val apiDetail = sampleApiDetail()
      val application1 = buildApplicationWithAccess(apiDetail)
      val application2 = buildApplicationWithoutAccess()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(Seq(application1, application2)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail, Seq(application1), Seq(application2))

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val apiDetail = sampleApiDetail()
      val application = buildApplicationWithoutAccess()
      val userAnswers = buildUserAnswers(apiDetail).set(AddAnApiSelectApplicationPage, application).toOption.value
      val fixture = buildFixture(Some(userAnswers))
      val filledForm = form.fill(application.id)

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(Seq(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, filledForm, NormalMode, apiDetail, Seq.empty, Seq(application))

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val apiDetail = sampleApiDetail()
      val application = buildApplicationWithoutAccess()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApplication(eqTo(application.id), any())(any()))
        .thenReturn(Future.successful(Some(application)))
      when(fixture.addAnApiSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiSelectApplicationController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", application.id))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }

    "must save the answer when valid data is submitted" in {
      val apiDetail = sampleApiDetail()
      val application = buildApplicationWithoutAccess()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApplication(eqTo(application.id), any())(any()))
        .thenReturn(Future.successful(Some(application)))
      when(fixture.addAnApiSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiSelectApplicationController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", application.id))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
          .set(AddAnApiContextPage, AddAnApi).toOption.value
          .set(AddAnApiApiPage, apiDetail).toOption.value
          .set(AddAnApiSelectApplicationPage, application).toOption.value

        verify(fixture.addAnApiSessionRepository).set(eqTo(expected))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val apiDetail = sampleApiDetail()
      val application = buildApplicationWithoutAccess()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))
      val boundForm = form.bind(Map("value" -> ""))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getApplications(eqTo(Some(FakeUser.email)), eqTo(false))(any()))
        .thenReturn(Future.successful(Seq(application)))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiSelectApplicationController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", ""))
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, boundForm, NormalMode, apiDetail, Seq.empty, Seq(application))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, routes.AddAnApiSelectApplicationController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiSelectApplicationController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", "answer"))
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return 404 Not Found page with suitable message when the selected application does not exist" in {
      val apiDetail = sampleApiDetail()
      val application = buildApplicationWithoutAccess()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApplication(eqTo(application.id), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.AddAnApiSelectApplicationController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", application.id))
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with ID ${application.id}.",
            Some(FakeUser)
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
    addAnApiSelectApplicationController: AddAnApiSelectApplicationController
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

    val controller = application.injector.instanceOf[AddAnApiSelectApplicationController]
    Fixture(application, apiHubService, addAnApiSessionRepository, controller)
  }

  private def buildView(
    application: PlayApplication,
    request: Request[?],
    form: Form[String],
    mode: Mode,
    apiDetail: ApiDetail,
    applicationsWithAccess: Seq[Application],
    applicationsWithoutAccess: Seq[Application]
  ): String = {
    application.injector.instanceOf[AddAnApiSelectApplicationView]
      .apply(
        form = form,
        mode = mode,
        user = Some(FakeUser),
        apiDetail = apiDetail,
        applicationsWithAccess = applicationsWithAccess,
        applicationsWithoutAccess = applicationsWithoutAccess
      )(request, messages(application))
      .toString()
  }

  private def buildApplicationWithAccess(apiDetail: ApiDetail): Application = {
    FakeApplication
      .copy(id = "test-application-id-1", name = "test-application-name-1")
      .addApi(Api(apiDetail.id, apiDetail.title, Seq.empty))
  }

  private def buildApplicationWithoutAccess(): Application = {
    FakeApplication
      .copy(id = "test-application-id-2", name = "test-application-name-2")
  }

  private def buildApplicationsWithoutAccess(count: Int): Seq[Application] = {
    Seq.tabulate(count) { i =>
      FakeApplication
        .copy(id = s"test-application-id-$i", name = s"test-application-name-$i")
    }
  }

  private def buildUserAnswers(apiDetail: ApiDetail): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(AddAnApiContextPage, AddAnApi).toOption.value
      .set(AddAnApiApiPage, apiDetail).toOption.value
  }

}
