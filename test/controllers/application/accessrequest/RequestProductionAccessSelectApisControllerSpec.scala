/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.application.accessrequest

import base.SpecBase
import controllers.actions.{FakeAccessRequest, FakeUser}
import fakes.FakeHipEnvironments
import forms.application.accessrequest.RequestProductionAccessSelectApisFormProvider
import models.NormalMode
import models.application.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.application.accessrequest.{RequestProductionAccessApisPage, RequestProductionAccessApplicationPage, RequestProductionAccessEnvironmentIdPage, RequestProductionAccessSelectApisPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AccessRequestSessionRepository
import viewmodels.application.{Accessible, ApplicationApi, ApplicationEndpoint, Inaccessible, TheoreticalScopes}
import views.html.application.accessrequest.RequestProductionAccessSelectApisView

import scala.concurrent.Future

class RequestProductionAccessSelectApisControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private lazy val requestProductionAccessSelectApisRoute = controllers.application.accessrequest.routes.RequestProductionAccessSelectApisController.onPageLoad(NormalMode).url

  private val testApplication = Application("id-1", "test", Creator("creator-email"), Seq(TeamMember("test-email")))
  private val endpointScopes = Seq("scope-1")
  private val theoreticalScopesNotApproved = TheoreticalScopes(endpointScopes.toSet, Map.empty)
  private val theoreticalScopesApproved = TheoreticalScopes(endpointScopes.toSet, Map(FakeHipEnvironments.production.id -> endpointScopes.toSet))

  private val applicationEndpoint = ApplicationEndpoint(
    httpMethod = "GET",
    path = "test-path",
    summary = None,
    description = None,
    scopes = endpointScopes,
    theoreticalScopes = theoreticalScopesApproved,
    pendingAccessRequests = Seq.empty
  )
  private val applicationEndpointNotAccessibleInProd = ApplicationEndpoint(
    httpMethod = "GET",
    path = "test-path",
    summary = None,
    description = None,
    scopes = endpointScopes,
    theoreticalScopes = theoreticalScopesNotApproved,
    pendingAccessRequests = Seq.empty
  )
  private val applicationApi =
    ApplicationApi(
      apiId = "api-id-1",
      apiTitle = "API title 1",
      apiNumber = None,
      endpoints = Seq(applicationEndpoint),
      pendingAccessRequests = Seq.empty,
      isMissing = false
    )
  private val applicationApiEndpointNotAccessible =
    ApplicationApi(
      apiId = "api-id-1",
      apiTitle = "API title 1",
      apiNumber = None,
      endpoints = Seq(applicationEndpointNotAccessibleInProd),
      pendingAccessRequests = Seq.empty,
      isMissing = false
    )
  private val applicationApiMissing =
    ApplicationApi(
      apiId = "api-id-2",
      apiTitle = "API title 2",
      apiNumber = None,
      endpoints = Seq(applicationEndpoint),
      pendingAccessRequests = Seq.empty,
      isMissing = true
    )
  private val applicationApiPendingRequest =
    ApplicationApi(
      apiId = "api-id-3",
      apiTitle = "API title 3",
      apiNumber = None,
      endpoints = Seq(applicationEndpoint),
      pendingAccessRequests = Seq(FakeAccessRequest),
      isMissing = false
    )
  private val applicationApis = Seq(
    applicationApi,
    applicationApiEndpointNotAccessible,
    applicationApiMissing,
    applicationApiPendingRequest,
  )

  private lazy val userAnswersWithApplicationAndApis = Some(
    emptyUserAnswers
      .set(RequestProductionAccessApplicationPage, testApplication)
      .flatMap(_.set(RequestProductionAccessApisPage, applicationApis))
      .get
  )

  private val userAnswersWithApplicationAndApisAndProductionlikeEnvironmentId = Some(
  userAnswersWithApplicationAndApis.value
    .set(RequestProductionAccessEnvironmentIdPage, "production")
    .get
  )

  private val userAnswersWithApplicationAndApisAndNonProductionlikeEnvironmentId = Some(
  userAnswersWithApplicationAndApis.value
    .set(RequestProductionAccessEnvironmentIdPage, "test")
    .get
  )


  val formProvider = new RequestProductionAccessSelectApisFormProvider()
  private val form = formProvider(applicationApis.toSet)

  "RequestProductionAccessSelectApis Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = userAnswersWithApplicationAndApisAndProductionlikeEnvironmentId)
        .build()

      running(application) {
        val request = FakeRequest(GET, requestProductionAccessSelectApisRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RequestProductionAccessSelectApisView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, Seq(applicationApiEndpointNotAccessible), Seq(applicationApiPendingRequest), FakeUser, FakeHipEnvironments.production)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithApplicationAndApisAndProductionlikeEnvironmentId.map(
        _.set(RequestProductionAccessSelectApisPage, Set(applicationApi.apiId)).success.value
      )

      val application = applicationBuilder(userAnswers = userAnswers)
        .build()

      running(application) {
        val request = FakeRequest(GET, requestProductionAccessSelectApisRoute)

        val view = application.injector.instanceOf[RequestProductionAccessSelectApisView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Set(applicationApiEndpointNotAccessible.apiId)), NormalMode, Seq(applicationApiEndpointNotAccessible), Seq(applicationApiPendingRequest), FakeUser, FakeHipEnvironments.production)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[AccessRequestSessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = userAnswersWithApplicationAndApisAndProductionlikeEnvironmentId)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AccessRequestSessionRepository].toInstance(mockSessionRepository),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, requestProductionAccessSelectApisRoute)
            .withFormUrlEncodedBody(("value[0]", applicationApiEndpointNotAccessible.apiId))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = userAnswersWithApplicationAndApisAndProductionlikeEnvironmentId)
        .build()

      running(application) {
        val request =
          FakeRequest(POST, requestProductionAccessSelectApisRoute)
            .withFormUrlEncodedBody(("value[0]", "invalid value"))

        val boundForm = form.bind(Map("value[0]" -> "invalid value"))

        val view = application.injector.instanceOf[RequestProductionAccessSelectApisView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Seq(applicationApiEndpointNotAccessible), Seq(applicationApiPendingRequest), FakeUser, FakeHipEnvironments.production)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, requestProductionAccessSelectApisRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no hip environment data is found" in {

      val application = applicationBuilder(userAnswers = userAnswersWithApplicationAndApis).build()

      running(application) {
        val request = FakeRequest(GET, requestProductionAccessSelectApisRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, requestProductionAccessSelectApisRoute)
            .withFormUrlEncodedBody(("value[0]", applicationApi.apiId))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if existing data is found but no environment id" in {

      val application = applicationBuilder(userAnswers = userAnswersWithApplicationAndApis).build()

      running(application) {
        val request =
          FakeRequest(POST, requestProductionAccessSelectApisRoute)
            .withFormUrlEncodedBody(("value[0]", applicationApi.apiId))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
