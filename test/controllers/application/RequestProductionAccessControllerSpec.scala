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
import forms.RequestProductionAccessDeclarationFormProvider
import models.NormalMode
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Api, Approved, Scope, SelectedEndpoint}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.{Accessible, ApplicationApi, ApplicationEndpoint, Inaccessible}
import views.html.ErrorTemplate
import views.html.application.RequestProductionAccessView

import scala.concurrent.Future

class RequestProductionAccessControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  private val form = new RequestProductionAccessDeclarationFormProvider()()

  "RequestProductionAccessController" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)

          val apiDetail = ApiDetail(
            id = "test-id",
            title = "test-title",
            description = "test-description",
            version = "test-version",
            endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", Some("A summary"), Some("A description"), Seq("test-scope"))))),
            shortDescription = None,
            openApiSpecification = "test-oas-spec"
          )

          val application = FakeApplication
            .addApi(Api(apiDetail.id, Seq(SelectedEndpoint("GET", "/test"))))
            .setSecondaryScopes(Seq(Scope("test-scope", Approved)))

          val applicationApis = Seq(
            ApplicationApi(apiDetail, Seq(ApplicationEndpoint("GET", "/test", Seq("test-scope"), Inaccessible, Accessible)))
          )

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(true))(any()))
            .thenReturn(Future.successful(Some(application)))
          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad(FakeApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[RequestProductionAccessView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(form, NormalMode, FakeApplication, applicationApis, Some(user))(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return the correct view when the applications has APIs" in {
      val fixture = buildFixture()

      val apiDetail = ApiDetail(
        id = "test-id",
        title = "test-title",
        description = "test-description",
        version = "test-version",
        endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", None, None, Seq("test-scope"))))),
        shortDescription = None,
        openApiSpecification = "test-oas-spec"
      )

      val application = FakeApplication
        .addApi(Api(apiDetail.id, Seq(SelectedEndpoint("GET", "/test"))))
        .setSecondaryScopes(Seq(Scope("test-scope", Approved)))

      val applicationApis = Seq(
        ApplicationApi(apiDetail, Seq(ApplicationEndpoint("GET", "/test", Seq("test-scope"), Inaccessible, Accessible)))
      )

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[RequestProductionAccessView]

        status(result) mustEqual OK
        contentAsString(result) mustBe view(form, NormalMode, application, applicationApis, Some(FakeUser))(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad(FakeApplication.id).url)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with Id ${FakeApplication.id}."
          )(request, messages(fixture.playApplication))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.application.routes.RequestProductionAccessController.onPageLoad(FakeApplication.id).url)
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