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
import controllers.routes
import forms.RequestProductionAccessDeclarationFormProvider
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application._
import models.user.UserModel
import models.{RequestProductionAccessDeclaration, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers, MockitoSugar}
import pages.{AccessRequestApplicationIdPage, RequestProductionAccessPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import play.api.{Application => PlayApplication}
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.{Accessible, ApplicationApi, ApplicationEndpoint, Inaccessible}
import views.html.application.RequestProductionAccessView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class RequestProductionAccessControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  private val form = new RequestProductionAccessDeclarationFormProvider()()
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "RequestProductionAccessController" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>

          val application = anApplication
          val userAnswers = buildUserAnswers(application)

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          val applicationApis = Seq(
            ApplicationApi(anApiDetail, Seq(ApplicationEndpoint("GET", "/test", Seq("test-scope"), Inaccessible, Accessible)))
          )

          when(fixture.apiHubService.getApiDetail(any())(any())).thenReturn(Future.successful(Some(anApiDetail)))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad().url)
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[RequestProductionAccessView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(form, FakeApplication, applicationApis, Some(user))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return the correct view when the applications has APIs" in {
      val application = anApplication
      val userAnswers = buildUserAnswers(application)

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      val applicationApis = Seq(
        ApplicationApi(anApiDetail, Seq(ApplicationEndpoint("GET", "/test", Seq("test-scope"), Inaccessible, Accessible)))
      )

      when(fixture.apiHubService.getApiDetail(any())(any())).thenReturn(Future.successful(Some(anApiDetail)))

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad().url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[RequestProductionAccessView]

        status(result) mustEqual OK
        contentAsString(result) mustBe view(form, application, applicationApis, Some(FakeUser))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to recovery page for a GET when there is no application in the session repository" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad().url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "must set the user answers and navigate to next page on submit" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>

          val application = anApplication
          val userAnswers = buildUserAnswers(application)

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.apiHubService.getApiDetail(any())(any())).thenReturn(Future.successful(Some(anApiDetail)))
          when(fixture.accessRequestSessionRepository.set(any())) thenReturn Future.successful(true)
          running(fixture.application) {
            val request = FakeRequest(POST, controllers.application.routes.RequestProductionAccessController.onSubmit().url).withFormUrlEncodedBody(("accept[0]", "accept"))
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER

            val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(fixture.accessRequestSessionRepository).set(captor.capture())
            val userAnswers: UserAnswers = captor.getValue
            userAnswers.get(RequestProductionAccessPage) mustEqual Some(Set(RequestProductionAccessDeclaration.Accept))
            redirectLocation(result) mustBe Some(controllers.application.routes.ProvideSupportingInformationController.onPageLoad().url)
          }
      }
    }
  }

  private def anApplication = {
    val apiDetail = anApiDetail

    val application = FakeApplication
      .addApi(Api(apiDetail.id, Seq(SelectedEndpoint("GET", "/test"))))
      .setSecondaryScopes(Seq(Scope("test-scope", Approved)))

    application
  }

  private def anApiDetail = {
    ApiDetail(
      id = "test-id",
      title = "test-title",
      description = "test-description",
      version = "test-version",
      endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", Some("A summary"), Some("A description"), Seq("test-scope"))))),
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

  private def buildFixture(userModel: UserModel = FakeUser, userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)): Fixture = {
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
  }
}
