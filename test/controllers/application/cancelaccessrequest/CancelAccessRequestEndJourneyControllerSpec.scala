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

package controllers.application.cancelaccessrequest

import base.SpecBase
import config.HipEnvironments
import controllers.actions.{FakeApplication, FakeUser}
import controllers.application.cancelaccessrequest.CancelAccessRequestEndJourneyController.Data
import fakes.FakeHipEnvironments
import models.accessrequest.{AccessRequest, Pending}
import models.application.*
import models.application.ApplicationLenses.ApplicationLensOps
import models.user.UserModel
import models.{CheckMode, RequestProductionAccessDeclaration, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import pages.QuestionPage
import pages.application.accessrequest.*
import pages.application.cancelaccessrequest.{CancelAccessRequestApplicationPage, CancelAccessRequestConfirmPage, CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.CancelAccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.*
import views.html.application.cancelaccessrequest.CancelAccessRequestSuccessView

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.concurrent.Future

class CancelAccessRequestEndJourneyControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  import CancelAccessRequestEndJourneyControllerSpec.*

  "CancelAccessRequestEndJourneyController" - {
    "must return OK and the correct view for a GET for a team member and make the request and clear user answers " in {

      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val application = anApplication
          val userAnswers = buildUserAnswers()
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          when(fixture.apiHubService.cancelAccessRequest(any(), any())(any())).thenReturn(Future.successful(()))
          when(fixture.cancelAccessRequestSessionRepository.clear(any())).thenReturn(Future.successful(true))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.cancelaccessrequest.routes.CancelAccessRequestEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            val view = fixture.application.injector.instanceOf[CancelAccessRequestSuccessView]
            status(result) mustEqual OK
            contentAsString(result) mustBe view(application, Some(user), AccessRequestsByEnvironment(Seq(anAccessRequest), FakeHipEnvironments))(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
            verify(fixture.apiHubService).cancelAccessRequest(eqTo(anAccessRequest.id), eqTo(user.email))(any())
            verify(fixture.cancelAccessRequestSessionRepository).clear(eqTo(user.userId))
          }
      }
    }

    "must redirect to journey recovery when no application in user answers" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val userAnswers = buildUserAnswers(without = Seq(RequestProductionAccessApplicationPage))
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
          }
      }
    }

    "must redirect to journey recovery when no Application APIs in user answers" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val userAnswers = buildUserAnswers(without = Seq(RequestProductionAccessApisPage))
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
          }
      }
    }

    "must redirect to request production access page when no confirm in user answers" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val userAnswers = buildUserAnswers(without = Seq(CancelAccessRequestConfirmPage))
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.cancelaccessrequest.routes.CancelAccessRequestEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.application.cancelaccessrequest.routes.CancelAccessRequestConfirmController.onPageLoad(CheckMode).url)
          }
      }
    }
  }

  private def buildFixture(userModel: UserModel, userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val cancelAccessRequestSessionRepository = mock[CancelAccessRequestSessionRepository]

    val playApplication = applicationBuilder(userAnswers = userAnswers, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[CancelAccessRequestSessionRepository].toInstance(cancelAccessRequestSessionRepository),
        bind[Clock].toInstance(clock)
      )
      .build()

    val controller = playApplication.injector.instanceOf[CancelAccessRequestEndJourneyController]
    Fixture(playApplication, apiHubService, cancelAccessRequestSessionRepository, controller)
  }

}

object CancelAccessRequestEndJourneyControllerSpec extends OptionValues{

  private val acceptRequestProductionAccessConditions: Set[RequestProductionAccessDeclaration] = Set(RequestProductionAccessDeclaration.Accept)
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val supportingInformation = "test-supporting-information"
  private val applicationApi = ApplicationApi(
    apiId = "test-api-id",
    apiTitle = "test-api-title",
    endpoints = Seq(
        ApplicationEndpoint(
          httpMethod = "GET",
          path = "/test",
          summary = None,
          description = None,
          scopes = Seq("scope"),
          theoreticalScopes = TheoreticalScopes(Set.empty, Map.empty),
          pendingAccessRequests = Seq.empty
        )
    ),
    pendingAccessRequests = Seq.empty,
    isMissing = false
  )

  private val anApi: Api = Api(applicationApi.apiId, applicationApi.apiTitle, Seq(SelectedEndpoint("GET", "/test"), SelectedEndpoint("POST", "/anothertest")))
  private val anApplication =
    FakeApplication
      .addApi(anApi)

  private val anAccessRequest = AccessRequest(
    "test-access-request-id",
    anApplication.id,
    anApi.id,
    anApi.title,
    Pending,
    Seq.empty,
    "abc",
    LocalDateTime.now(clock),
    "me",
    None,
    None,
    "test"
  )


  private val data: Data = Data(
    application = anApplication,
    accessRequests = Seq(anAccessRequest),
    selectedAccessRequests = Set(anAccessRequest.id)
  )

  private def buildUserAnswers(without: Seq[QuestionPage[?]] = Seq.empty): UserAnswers = {
    val fullUserAnswers = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(CancelAccessRequestApplicationPage, data.application).toOption.value
      .set(CancelAccessRequestPendingPage, data.accessRequests).toOption.value
      .set(CancelAccessRequestSelectApiPage, data.selectedAccessRequests).toOption.value
      .set(CancelAccessRequestConfirmPage, true).toOption.value

    without.foldRight(fullUserAnswers)((page, userAnswers) => userAnswers.remove(page).toOption.value)
  }

  private case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService,
    cancelAccessRequestSessionRepository: CancelAccessRequestSessionRepository,
    cancelAccessRequestController: CancelAccessRequestEndJourneyController
  )

}
