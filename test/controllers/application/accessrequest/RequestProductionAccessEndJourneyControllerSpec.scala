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
import controllers.application.accessrequest.RequestProductionAccessEndJourneyController.Data
import fakes.FakeHipEnvironments
import models.accessrequest.{AccessRequestApi, AccessRequestEndpoint, AccessRequestRequest}
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
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.application.*
import views.html.application.accessrequest.RequestProductionAccessSuccessView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class RequestProductionAccessEndJourneyControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  import RequestProductionAccessEndJourneyControllerSpec.*

  "RequestProductionAccessEndJourneyController" - {
    "must return OK and the correct view for a GET for a team member or supporter and make the request and clear user answers " in {

      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val application = anApplication
          val userAnswers = buildUserAnswers()
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))
          val accessRequestRequest = data.toRequest(user.email, FakeHipEnvironments.production.id)

          when(fixture.apiHubService.requestProductionAccess(any())(any())).thenReturn(Future.successful(()))
          when(fixture.accessRequestSessionRepository.clear(any())).thenReturn(Future.successful(true))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            val view = fixture.application.injector.instanceOf[RequestProductionAccessSuccessView]
            status(result) mustEqual OK
            contentAsString(result) mustBe view(application, Some(user), accessRequestRequest.apis)(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
            verify(fixture.apiHubService).requestProductionAccess(eqTo(accessRequestRequest))(any())
            verify(fixture.accessRequestSessionRepository).clear(eqTo(user.userId))
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

    "must redirect to request production access page when no accept conditions in user answers" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val userAnswers = buildUserAnswers(without = Seq(RequestProductionAccessPage))
          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.application.accessrequest.routes.RequestProductionAccessController.onPageLoad().url)
          }
      }
    }

    "must redirect to provide supporting information page when no supporting information" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val userAnswers = buildUserAnswers(without = Seq(ProvideSupportingInformationPage))

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(CheckMode).url)
          }
      }
    }

    "must redirect to Select APIs page when no answer exists" in {
      forAll(teamMemberAndSupporterTable) {
        (user: UserModel) =>
          val userAnswers = buildUserAnswers(without = Seq(RequestProductionAccessSelectApisPage))

          val fixture = buildFixture(userModel = user, userAnswers = Some(userAnswers))

          running(fixture.application) {
            val request = FakeRequest(GET, controllers.application.accessrequest.routes.RequestProductionAccessEndJourneyController.submitRequest().url)
            val result = route(fixture.application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.application.accessrequest.routes.RequestProductionAccessSelectApisController.onPageLoad(CheckMode).url)
          }
      }
    }
  }

  "RequestProductionAccessEndJourneyController.Data.toRequest" - {
    "must build a request for selected APIs only" in {
      val selectedApplicationApi = buildApplicationApi(1, Seq(Inaccessible))
      val nonSelectedApplicationApi = buildApplicationApi(2, Seq(Inaccessible))

      val data = Data(
        application = anApplication,
        applicationApis = Seq(selectedApplicationApi, nonSelectedApplicationApi),
        selectedApis = Set(selectedApplicationApi.apiId),
        supportingInformation = supportingInformation
      )

      val expected = AccessRequestRequest(
        applicationId = anApplication.id,
        supportingInformation = supportingInformation,
        requestedBy = FakeUser.email,
        apis = Seq(
          AccessRequestApi(
            apiId = selectedApplicationApi.apiId,
            apiName = selectedApplicationApi.apiTitle,
            endpoints = selectedApplicationApi.endpoints.map(
              endpoint =>
                AccessRequestEndpoint(
                  httpMethod = endpoint.httpMethod,
                  path = endpoint.path,
                  scopes = endpoint.scopes
                )
            )
          )
        ),
        environmentId = Some(FakeHipEnvironments.production.id)
      )

      val actual = data.toRequest(FakeUser.email, FakeHipEnvironments.production.id)

      actual mustBe expected
    }

    "must only include endpoints that are inaccessible in production" in {
      val selectedApplicationApi = buildApplicationApi(1, Seq(Inaccessible, Accessible))

      val data = Data(
        application = anApplication,
        applicationApis = Seq(selectedApplicationApi),
        selectedApis = Set(selectedApplicationApi.apiId),
        supportingInformation = supportingInformation
      )

      val expected = AccessRequestRequest(
        applicationId = anApplication.id,
        supportingInformation = supportingInformation,
        requestedBy = FakeUser.email,
        apis = Seq(
          AccessRequestApi(
            apiId = selectedApplicationApi.apiId,
            apiName = selectedApplicationApi.apiTitle,
            endpoints = selectedApplicationApi.endpoints.filter(!_.productionAccess.isAccessible).map(
              endpoint =>
                AccessRequestEndpoint(
                  httpMethod = endpoint.httpMethod,
                  path = endpoint.path,
                  scopes = endpoint.scopes
                )
            )
          )
        ),
        environmentId = Some(FakeHipEnvironments.production.id)
      )

      val actual = data.toRequest(FakeUser.email, FakeHipEnvironments.production.id)

      actual mustBe expected
    }
  }

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

}

object RequestProductionAccessEndJourneyControllerSpec extends OptionValues{

  private val acceptRequestProductionAccessConditions: Set[RequestProductionAccessDeclaration] = Set(RequestProductionAccessDeclaration.Accept)
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val supportingInformation = "test-supporting-information"
  private val applicationApi = buildApplicationApi(1, Seq(Inaccessible))

  private val anApplication =
    FakeApplication
      .addApi(Api(applicationApi.apiId, applicationApi.apiTitle, Seq(SelectedEndpoint("GET", "/test"), SelectedEndpoint("POST", "/anothertest"))))

  private def buildApplicationApi(apiId: Int, endpointAccesses: Seq[ApplicationEndpointAccess]): ApplicationApi = {
    ApplicationApi(
      apiId = s"test-api-id-$apiId",
      apiTitle = s"test-api-title-$apiId",
      totalEndpoints = endpointAccesses.size,
      endpoints = endpointAccesses.map(
        endpointAccess =>
          ApplicationEndpoint(
            httpMethod = "GET",
            path = s"/test/$apiId/$endpointAccess",
            summary = None,
            description = None,
            scopes = Seq(s"test-scope-$apiId-$endpointAccess"),
            productionAccess = endpointAccess,
            nonProductionAccess = Accessible
          )
      ),
      pendingAccessRequestCount = 0,
      isMissing = false
    )
  }

  private val data: Data = Data(
    application = anApplication,
    applicationApis = Seq(applicationApi),
    selectedApis = Set(applicationApi.apiId),
    supportingInformation = supportingInformation
  )

  private def buildUserAnswers(without: Seq[QuestionPage[?]] = Seq.empty): UserAnswers = {
    val fullUserAnswers = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
      .set(RequestProductionAccessApplicationPage, data.application).toOption.value
      .set(RequestProductionAccessApisPage, data.applicationApis).toOption.value
      .set(RequestProductionAccessSelectApisPage, data.selectedApis).toOption.value
      .set(ProvideSupportingInformationPage, data.supportingInformation).toOption.value
      .set(RequestProductionAccessPage, acceptRequestProductionAccessConditions).toOption.value

    without.foldRight(fullUserAnswers)((page, userAnswers) => userAnswers.remove(page).toOption.value)
  }

  private case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService,
    accessRequestSessionRepository: AccessRequestSessionRepository,
    requestProductionAccessController: RequestProductionAccessController
  )

}
