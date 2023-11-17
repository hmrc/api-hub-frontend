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
import models.{NormalMode, UserAnswers}
import models.api.{ApiDetail, Endpoint, EndpointMethod}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Api, Application, Approved, Scope, SelectedEndpoint}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import pages.AccessRequestApplicationIdPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.{Accessible, ApplicationApi, ApplicationEndpoint, Inaccessible}
import views.html.ErrorTemplate
import views.html.application.{ProvideSupportingInformationView, RequestProductionAccessView}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ProvideSupportingInformationControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  private val form = new RequestProductionAccessDeclarationFormProvider()()
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "ProvideSupportingInformationController" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val application = anApplication
          val userAnswers = buildUserAnswers(application)
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))



          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), any())(any()))
            .thenReturn(Future.successful(Some(application)))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.routes.ProvideSupportingInformationController.onPageLoad.url)
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[ProvideSupportingInformationView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(form, Some(user))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }


    "must redirect to Unauthorised page for a GET when user is not a team member or supporter" in {
      val fixture = buildFixture(userModel = FakeUserNotTeamMember)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        val request = FakeRequest(GET, controllers.application.routes.ProvideSupportingInformationController.onPageLoad.url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
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
                              provideSupportingInformationController: ProvideSupportingInformationController
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

    val controller = playApplication.injector.instanceOf[ProvideSupportingInformationController]
    Fixture(playApplication, apiHubService, accessRequestSessionRepository, controller)
  }

  private def buildUserAnswers(application: Application): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(AccessRequestApplicationIdPage, application).toOption.value
  }
}
