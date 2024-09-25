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
import forms.RequestProductionAccessDeclarationFormProvider
import models.accessrequest.Pending
import models.api.*
import models.application.*
import models.application.ApplicationLenses.ApplicationLensOps
import models.user.UserModel
import models.{RequestProductionAccessDeclaration, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.application.accessrequest.*
import play.api.Application as PlayApplication
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.*
import viewmodels.checkAnswers.application.accessrequest.{ProvideSupportingInformationSummary, RequestProductionAccessApplicationSummary, RequestProductionAccessSelectApisSummary}
import views.html.application.RequestProductionAccessView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class RequestProductionAccessControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  private val form = new RequestProductionAccessDeclarationFormProvider()()
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val supportingInformation = "test-supporting-information"
  private val onwardRoute = controllers.routes.IndexController.onPageLoad

  "RequestProductionAccessController" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>

          val application = anApplication
          val userAnswers = buildUserAnswers(application)

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          val applicationApis = Seq(applicationApi(application))

          when(fixture.apiHubService.getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(Some(Pending)))(any()))
            .thenReturn(Future.successful(Seq.empty))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad().url)
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[RequestProductionAccessView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(form, buildSummaries(userAnswers)(messages(fixture.application)), applicationApis, Some(user))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return the correct view when the applications has APIs" in {
      val application = anApplication
      val userAnswers = buildUserAnswers(application)

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      val applicationApis = Seq(
        ApplicationApi(anApiDetail, Seq(ApplicationEndpoint("GET", "/test", Some("A summary"), Some("A description"), Seq("test-scope"), Inaccessible, Accessible)), false)
      )

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(true), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(application.id)), eqTo(Some(Pending)))(any()))
        .thenReturn(Future.successful(Seq.empty))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad().url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[RequestProductionAccessView]

        status(result) mustEqual OK
        contentAsString(result) mustBe view(form, buildSummaries(userAnswers)(messages(fixture.application)), applicationApis, Some(FakeUser))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must set the user answers and navigate to next page on submit" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>

          val application = anApplication
          val userAnswers = buildUserAnswers(application)

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.apiHubService.getApiDetail(any())(any())).thenReturn(Future.successful(Some(anApiDetail)))
          when(fixture.accessRequestSessionRepository.set(any())).thenReturn(Future.successful(true))

          running(fixture.application) {
            val request = FakeRequest(POST, controllers.application.routes.RequestProductionAccessController.onSubmit().url).withFormUrlEncodedBody(("accept[0]", "accept"))
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(onwardRoute.url)

            val expected = userAnswers.set(RequestProductionAccessPage, Set(RequestProductionAccessDeclaration.Accept)).toOption.value
            verify(fixture.accessRequestSessionRepository).set(eqTo(expected))
          }
      }
    }
  }

  private def anApplication = {
    val apiDetail = anApiDetail

    val application = FakeApplication
      .addApi(Api(apiDetail.id, apiDetail.title, Seq(SelectedEndpoint("GET", "/test"))))
      .setSecondaryScopes(Seq(Scope("test-scope")))

    application
  }

  private def anApiDetail = {
    ApiDetail(
      id = "test-id",
      publisherReference = "test-pub-ref",
      title = "test-title",
      description = "test-description",
      version = "test-version",
      endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", Some("A summary"), Some("A description"), Seq("test-scope"))))),
      shortDescription = None,
      openApiSpecification = "test-oas-spec",
      apiStatus = Live,
      reviewedDate = Instant.now(),
      platform = "HIP",
      maintainer = Maintainer("name", "#slack", List.empty)
    )
  }

  private def applicationApi(application: Application) =
    ApplicationApi(
      apiId = anApiDetail.id,
      apiTitle = anApiDetail.title,
      totalEndpoints = anApiDetail.endpoints.size,
      endpoints = anApiDetail.endpoints.flatMap(
        endpoint =>
          endpoint.methods.map(
            method =>
              ApplicationEndpoint(
                httpMethod = method.httpMethod,
                path = endpoint.path,
                summary = method.summary,
                description = method.description,
                scopes = method.scopes,
                primaryAccess = ApplicationEndpointAccess(application, false, method, Primary),
                secondaryAccess = Inaccessible
              )
          )
      ),
      hasPendingAccessRequest = false,
      isMissing = false
    )

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
        bind[Clock].toInstance(clock),
        bind[Navigator].toInstance(FakeNavigator(onwardRoute))
      )
      .build()

    val controller = playApplication.injector.instanceOf[RequestProductionAccessController]
    Fixture(playApplication, apiHubService, accessRequestSessionRepository, controller)
  }

  private def buildUserAnswers(application: Application): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(RequestProductionAccessApplicationPage, application).toOption.value
      .set(RequestProductionAccessApisPage, Seq(applicationApi(application))).toOption.value
      .set(RequestProductionAccessSelectApisPage, Set(applicationApi(application).apiId)).toOption.value
      .set(ProvideSupportingInformationPage, supportingInformation).toOption.value
  }

  private def buildSummaries(userAnswers: UserAnswers)(implicit messages: Messages) = {
    Seq(
      RequestProductionAccessApplicationSummary.row(userAnswers),
      RequestProductionAccessSelectApisSummary.row(userAnswers),
      ProvideSupportingInformationSummary.row(userAnswers)
    )
  }

}
