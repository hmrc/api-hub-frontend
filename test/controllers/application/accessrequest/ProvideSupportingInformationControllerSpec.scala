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

package controllers.application.accessrequest

import base.SpecBase
import controllers.actions.{FakeApplication, FakeUser}
import forms.application.accessrequest.ProvideSupportingInformationFormProvider
import models.{NormalMode, UserAnswers}
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live, Maintainer}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.*
import models.user.UserModel
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.application.accessrequest.{ProvideSupportingInformationPage, RequestProductionAccessApplicationPage}
import play.api.data.FormError
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.application.accessrequest.ProvideSupportingInformationView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ProvideSupportingInformationControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  private val form = new ProvideSupportingInformationFormProvider()()
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val onwardRoute = controllers.routes.IndexController.onPageLoad

  "ProvideSupportingInformationController" - {
    "must return OK and the correct view for a GET for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val application = anApplication
          val userAnswers = buildUserAnswers(application)
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any(), any())(any()))
            .thenReturn(Future.successful(Some(application)))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(NormalMode).url)
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[ProvideSupportingInformationView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(form, NormalMode, Some(user))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return OK and the correct view for a GET for a team member or supporter when the question has already been answered" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val application = anApplication
          val userAnswers = buildUserAnswers(application).set(ProvideSupportingInformationPage, "blah").toOption.value
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any(), any())(any()))
            .thenReturn(Future.successful(Some(application)))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(NormalMode).url)
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[ProvideSupportingInformationView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(form.fill("blah"), NormalMode, Some(user))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
            contentAsString(result).contains("blah") mustBe true
          }
      }
    }

    "must return 400 Bad Request and the correct view for a GET for a team member or supporter when the form contains errors" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val application = anApplication
          val userAnswers = buildUserAnswers(application)
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any(), any())(any()))
            .thenReturn(Future.successful(Some(application)))

          running(fixture.application) {
            val request = FakeRequest(POST, controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(NormalMode).url)

            val formWithError = form.withError(FormError("value", "Enter information to support your request"))
            val result = route(fixture.application, request).value

            val view = fixture.application.injector.instanceOf[ProvideSupportingInformationView]

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustBe view(formWithError, NormalMode, Some(user))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must set the user answers and navigate to next page on submit" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>

          val application = anApplication
          val userAnswers = buildUserAnswers(application)

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.accessRequestSessionRepository.set(any())).thenReturn(Future.successful(true))
          running(fixture.application) {
            val request = FakeRequest(POST, controllers.application.accessrequest.routes.ProvideSupportingInformationController.onSubmit(NormalMode).url).withFormUrlEncodedBody(("value", "blah"))
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(fixture.accessRequestSessionRepository).set(captor.capture())
            val userAnswers: UserAnswers = captor.getValue
            userAnswers.get(ProvideSupportingInformationPage).value mustBe "blah"
            redirectLocation(result) mustBe Some(onwardRoute.url)
          }
      }
    }
  }

  private def anApplication = {
    val apiDetail = anApiDetail

    val application = FakeApplication
      .addApi(Api(apiDetail.id, apiDetail.title, Seq(SelectedEndpoint("GET", "/test"))))

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

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              accessRequestSessionRepository: AccessRequestSessionRepository,
                              provideSupportingInformationController: ProvideSupportingInformationController
                            )

  private def buildFixture(userModel: UserModel, userAnswers: Option[UserAnswers]): Fixture = {
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

    val controller = playApplication.injector.instanceOf[ProvideSupportingInformationController]
    Fixture(playApplication, apiHubService, accessRequestSessionRepository, controller)
  }

  private def buildUserAnswers(application: Application): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(RequestProductionAccessApplicationPage, application).toOption.value
  }

}
