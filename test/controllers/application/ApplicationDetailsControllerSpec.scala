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
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import fakes.FakeHipEnvironments
import models.accessrequest.Pending
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live, Maintainer}
import models.application.{Api, SelectedEndpoint, TeamMember}
import models.application.ApplicationLenses.ApplicationLensOps
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.{Accessible, ApplicationApi, ApplicationDetailsViewModel, ApplicationEndpoint, Inaccessible, TheoreticalScopes}
import views.html.ErrorTemplate
import views.html.application.ApplicationDetailsView

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

class ApplicationDetailsControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation{

  "ApplicationDetails Controller" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false))(any()))
            .thenReturn(Future.successful(Some(FakeApplication)))

          when(fixture.apiHubService.getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(None))(any()))
            .thenReturn(Future.successful(Seq.empty))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]
            val viewModel = ApplicationDetailsViewModel(FakeApplication, Seq.empty, Some(user), FakeHipEnvironments)

            status(result) mustEqual OK
            contentAsString(result) mustBe view(viewModel)(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return OK and the correct view for a GET when the application has a global team" in {
      val fixture = buildFixture()

      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email)))
      val application = FakeApplication.setTeamId(team.id).setTeamName(team.name)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(application.id)), eqTo(None))(any()))
        .thenReturn(Future.successful(Seq.empty))

      when(fixture.apiHubService.findTeamById(eqTo(team.id))(any))
        .thenReturn(Future.successful(Some(team)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]
        val viewModel = ApplicationDetailsViewModel(application, Seq.empty, Some(FakeUser), FakeHipEnvironments)

        status(result) mustEqual OK
        contentAsString(result) mustBe view(viewModel)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must sort the application's team members alphabetically" in {
      val application = FakeApplication.copy(
        teamMembers = Seq(
          TeamMember(email = FakeUser.email),
          TeamMember(email = "cc@hmrc.gov.uk"),
          TeamMember(email = "ab@hmrc.gov.uk"),
          TeamMember(email = "aa@hmrc.gov.uk"),
          TeamMember(email = "Zb@hmrc.gov.uk"),
          TeamMember(email = "za@hmrc.gov.uk")
        )
      )

      val expected = FakeApplication.copy(
        teamMembers = Seq(
          TeamMember(email = "aa@hmrc.gov.uk"),
          TeamMember(email = "ab@hmrc.gov.uk"),
          TeamMember(email = "cc@hmrc.gov.uk"),
          TeamMember(email = FakeUser.email),
          TeamMember(email = "za@hmrc.gov.uk"),
          TeamMember(email = "Zb@hmrc.gov.uk")
        )
      )

      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(application.id), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(application.id)), eqTo(None))(any()))
        .thenReturn(Future.successful(Seq.empty))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]
        val viewModel = ApplicationDetailsViewModel(expected, Seq.empty, Some(FakeUser), FakeHipEnvironments)

        status(result) mustBe OK
        contentAsString(result) mustBe view(viewModel)(request, messages(fixture.playApplication)).toString
      }
    }

    "must return the correct view when the application has APIs added" in {
      val fixture = buildFixture()

      val apiDetail = ApiDetail(
        id = "test-id",
        publisherReference = "test-pub-ref",
        title = "test-title",
        description = "test-description",
        version = "test-version",
        endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", None, None, Seq("test-scope"))))),
        shortDescription = None,
        openApiSpecification = "test-oas-spec",
        apiStatus = Live,
        created = Instant.now(),
        reviewedDate = Instant.now(),
        platform = "HIP",
        maintainer = Maintainer("name", "#slack", List.empty)
      )

      val application = FakeApplication
        .addApi(Api(apiDetail.id, apiDetail.title, Seq(SelectedEndpoint("GET", "/test"))))

      val applicationApis = Seq(
        ApplicationApi(apiDetail, Seq(ApplicationEndpoint("GET", "/test", None, None, Seq("test-scope"), TheoreticalScopes(Seq("test-scope").toSet, Map.empty), Seq.empty)), Seq.empty)
      )

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(application.id)), eqTo(None))(any()))
        .thenReturn(Future.successful(Seq.empty))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]
        val viewModel = ApplicationDetailsViewModel(application, applicationApis, Some(FakeUser), FakeHipEnvironments)

        status(result) mustEqual OK
        contentAsString(result) mustBe view(viewModel)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return the correct view when the APIs are missing" in {
      val fixture = buildFixture()
      val apiId = "test-id"
      val apiTitle = "test-title"

      val api = Api(apiId, apiTitle, Seq(SelectedEndpoint("GET", "/test")))

      val application = FakeApplication
        .addApi(api)

      val applicationApis = Seq(
        ApplicationApi(api, Seq.empty)
      )

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(application.id)), eqTo(None))(any()))
        .thenReturn(Future.successful(Seq.empty))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ApplicationDetailsView]
        val viewModel = ApplicationDetailsViewModel(application, applicationApis, Some(FakeUser), FakeHipEnvironments)

        status(result) mustEqual OK
        contentAsString(result) mustBe view(viewModel)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with ID ${FakeApplication.id}.",
            Some(FakeUser)
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.ApplicationDetailsController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
