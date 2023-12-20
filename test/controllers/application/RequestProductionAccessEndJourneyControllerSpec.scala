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

package controllers.application

import base.SpecBase
import controllers.actions.{FakeApplication, FakeUser}
import models.accessrequest.{AccessRequestApi, AccessRequestEndpoint, AccessRequestRequest}
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application._
import models.user.UserModel
import models.{RequestProductionAccessDeclaration, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import pages.{AccessRequestApplicationIdPage, ProvideSupportingInformationPage, RequestProductionAccessPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import play.api.{Application => PlayApplication}
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.application.RequestProductionAccessSuccessView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class RequestProductionAccessEndJourneyControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  val acceptRequestProductionAccessConditions: Set[RequestProductionAccessDeclaration] = Set(RequestProductionAccessDeclaration.Accept)

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "RequestProductionAccessEndJourneyController" - {
    "must return OK and the correct view for a GET for a team member or supporter and make the request and clear user answers " in {

      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val application = anApplication
          val userAnswers = buildUserAnswers(application)
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))
          val apiDetail = anApiDetail

          // 2 endpoints for the API, because the application currently holds neither scope
          val accessRequestApis = Seq(
            AccessRequestApi(apiDetail.id, apiDetail.title, Seq(AccessRequestEndpoint("GET", "/test", Seq("test-scope")),
            AccessRequestEndpoint("POST", "/anothertest", Seq("another-test-scope"))))
          )
          val expectedAccessRequest: AccessRequestRequest = AccessRequestRequest(application.id, "blah", user.email.get, accessRequestApis)

          when(fixture.apiHubService.hasPendingAccessRequest(any())(any())).thenReturn(Future.successful(false))
          when(fixture.apiHubService.getApiDetail(any())(any())).thenReturn(Future.successful(Some(anApiDetail)))
          when(fixture.apiHubService.requestProductionAccess(ArgumentMatchers.eq(expectedAccessRequest))(any())).thenReturn(Future.successful(()))
          when(fixture.accessRequestSessionRepository.clear(user.userId)).thenReturn(Future.successful(true))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            val view = fixture.application.injector.instanceOf[RequestProductionAccessSuccessView]
            status(result) mustEqual OK
            contentAsString(result) mustBe view(application, Some(user), accessRequestApis)(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
            verify(fixture.apiHubService).requestProductionAccess(ArgumentMatchers.eq(expectedAccessRequest))(any())
            verify(fixture.accessRequestSessionRepository).clear(ArgumentMatchers.eq(user.userId))
          }
      }
    }

    "will not request access for API endpoints whose scope is already held by the application " in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val application = anApplication.setPrimaryScopes(Seq(Scope("test-scope"))) //decorate application with primary scope test-scope
          val userAnswers = buildUserAnswers(application)
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))
          val apiDetail = anApiDetail

          // Only one endpoint, because we already hold test-scope
          val accessRequestApis = Seq(
            AccessRequestApi(apiDetail.id, apiDetail.title, Seq(AccessRequestEndpoint("POST", "/anothertest", Seq("another-test-scope"))))
          )
          val expectedAccessRequest: AccessRequestRequest = AccessRequestRequest(application.id, "blah", user.email.get, accessRequestApis)

          when(fixture.apiHubService.hasPendingAccessRequest(any())(any())).thenReturn(Future.successful(false))
          when(fixture.apiHubService.getApiDetail(any())(any())).thenReturn(Future.successful(Some(anApiDetail)))
          when(fixture.apiHubService.requestProductionAccess(ArgumentMatchers.eq(expectedAccessRequest))(any())).thenReturn(Future.successful(()))
          when(fixture.accessRequestSessionRepository.clear(user.userId)).thenReturn(Future.successful(true))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            val view = fixture.application.injector.instanceOf[RequestProductionAccessSuccessView]
            status(result) mustEqual OK
            contentAsString(result) mustBe view(application, Some(user), accessRequestApis)(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
            verify(fixture.apiHubService).requestProductionAccess(ArgumentMatchers.eq(expectedAccessRequest))(any())
            verify(fixture.accessRequestSessionRepository).clear(ArgumentMatchers.eq(user.userId))
          }
      }
    }

    "must redirect to journey recovery when no application in user answers" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>

          val userAnswers = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
          }
      }
    }

    "must redirect to request production access page when no accept conditions in user answers" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>

          val application = anApplication
          val userAnswers = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant()).set(AccessRequestApplicationIdPage, application).toOption.value
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.application.routes.RequestProductionAccessController.onPageLoad().url)
          }
      }
    }

    "must redirect to provide supporting information page when no supporting information" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>

          val application = anApplication
          val userAnswers = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
            .set(AccessRequestApplicationIdPage, application).toOption.value
            .set(RequestProductionAccessPage, acceptRequestProductionAccessConditions).toOption.value

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.application.routes.ProvideSupportingInformationController.onPageLoad().url)
          }
      }
    }

    "must redirect to journey recovery when no user email" in {

      val user = FakeUser.copy(email = None)
      val fixture = buildFixture(userModel = user, userAnswers = None)

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
    }
  }

  private def anApplication = {
    val apiDetail = anApiDetail

    val application = FakeApplication
      .addApi(Api(apiDetail.id, Seq(SelectedEndpoint("GET", "/test"), SelectedEndpoint("POST", "/anothertest"))))
      .setSecondaryScopes(Seq(Scope("test-scope")))
    application
  }

  private def anApiDetail = {
    ApiDetail(
      id = "test-id",
      title = "test-title",
      description = "test-description",
      version = "test-version",
      endpoints = Seq(
        Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", Some("A summary"), Some("A description"), Seq("test-scope")))),
        Endpoint(path = "/anothertest", methods = Seq(EndpointMethod("POST", Some("A summary"), Some("A description"), Seq("another-test-scope"))))),
      shortDescription = None,
      openApiSpecification = "test-oas-spec"
    )
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              accessRequestSessionRepository: AccessRequestSessionRepository,
                              requestProductionAccessController: RequestProductionAccessController
                            )

  private def buildFixture(userModel: UserModel, userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val accessRequestSessionRepository = mock[AccessRequestSessionRepository]

    val playApplication = applicationBuilder(userAnswers = userAnswers, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[AccessRequestSessionRepository].toInstance(accessRequestSessionRepository),
        bind[Clock].toInstance(clock)
      )
      .build()

    val controller = playApplication.injector.instanceOf[RequestProductionAccessController]
    Fixture(playApplication, apiHubService, accessRequestSessionRepository, controller)
  }

  private def buildUserAnswers(application: Application): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(AccessRequestApplicationIdPage, application).toOption.value
      .set(RequestProductionAccessPage, acceptRequestProductionAccessConditions).toOption.value
      .set(ProvideSupportingInformationPage, "blah").toOption.value
  }
}
