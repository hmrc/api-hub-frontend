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
import forms.ApiPolicyConditionsDeclarationPageFormProvider
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.{ApiPolicyConditionsDeclaration, Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ApiPolicyConditionsDeclarationPageView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ApiPolicyConditionsDeclarationPageControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  private val nextPage = routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val form = new ApiPolicyConditionsDeclarationPageFormProvider()()

  "ApiPolicyConditionsDeclarationPageController" - {

    "must return OK and the correct view for a GET when the user has selected an application" in {
      val apiDetail = sampleApiDetail()
      val application = FakeApplication
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getUserApplications(ArgumentMatchers.eq(FakeUser.email.value), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Seq(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail)

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET" in {
      val apiDetail = sampleApiDetail()
      val application = FakeApplication
      val userAnswers = buildUserAnswers(apiDetail).set(AddAnApiSelectApplicationPage, application.id).toOption.value
      val fixture = buildFixture(Some(userAnswers))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.apiHubService.getUserApplications(ArgumentMatchers.eq(FakeUser.email.value), ArgumentMatchers.eq(true))(any()))
        .thenReturn(Future.successful(Seq(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value
        val view = buildView(fixture.application, request, form, NormalMode, apiDetail)

        status(result) mustBe OK
        contentAsString(result) mustBe view
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val apiDetail = sampleApiDetail()
      val fixture = buildFixture(Some(buildUserAnswers(apiDetail)))

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))

      when(fixture.addAnApiSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, routes.ApiPolicyConditionsDeclarationPageController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value[0]", "accept"))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }


    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(NormalMode).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(POST, routes.ApiPolicyConditionsDeclarationPageController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", "answer"))
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  private case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService,
    addAnApiSessionRepository: AddAnApiSessionRepository,
    apiPolicyConditionsDeclarationPageController: ApiPolicyConditionsDeclarationPageController
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

    val controller = application.injector.instanceOf[ApiPolicyConditionsDeclarationPageController]
    Fixture(application, apiHubService, addAnApiSessionRepository, controller)
  }

  private def buildView(
    application: PlayApplication,
    request: Request[_],
    form: Form[Set[ApiPolicyConditionsDeclaration]],
    mode: Mode,
    apiDetail: ApiDetail
  ): String = {
    application.injector.instanceOf[ApiPolicyConditionsDeclarationPageView]
      .apply(
        form = form,
        mode = mode,
        apiDetail = apiDetail
      )(request, messages(application))
      .toString()
  }

  private def buildUserAnswers(apiDetail: ApiDetail): UserAnswers = {
    UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(AddAnApiApiIdPage, apiDetail.id).toOption.value
  }

}
