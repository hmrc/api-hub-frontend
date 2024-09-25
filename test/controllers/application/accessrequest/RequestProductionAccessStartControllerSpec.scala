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
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import controllers.helpers.ApplicationApiBuilder
import generators.ApiDetailGenerators
import models.api.*
import models.application.*
import models.application.ApplicationLenses.ApplicationLensOps
import models.user.UserModel
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.application.accessrequest.{RequestProductionAccessApisPage, RequestProductionAccessApplicationPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.application.ApplicationApi

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class RequestProductionAccessStartControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  import RequestProductionAccessStartControllerSpec.*

  "RequestProductionAccessStartController" - {
    "must initiate user answers with the application and persist this in the session repository" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.applicationApiBuilder.build(eqTo(application))(any))
        .thenReturn(Future.successful(Seq(applicationApi)))

      when(fixture.accessRequestSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.RequestProductionAccessStartController.onPageLoad(application.id).url)

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
          .set(RequestProductionAccessApplicationPage, application)
          .flatMap(_.set(RequestProductionAccessApisPage, Seq(applicationApi)))
          .toOption.value

        verify(fixture.accessRequestSessionRepository).set(eqTo(expected))
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.applicationApiBuilder.build(eqTo(application))(any))
        .thenReturn(Future.successful(Seq(applicationApi)))

      when(fixture.accessRequestSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.RequestProductionAccessStartController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RequestProductionAccessSelectApisController.onPageLoad(NormalMode).url)
      }
    }

    "must redirect to unauthorised" in {
      val fixture = buildFixture(FakeUserNotTeamMember)
      val application: Application = FakeApplication

      when(fixture.apiHubService.getApplication(eqTo(application.id), any(), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, routes.RequestProductionAccessStartController.onPageLoad(application.id).url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    accessRequestSessionRepository: AccessRequestSessionRepository,
    applicationApiBuilder: ApplicationApiBuilder,
    requestProductionAccessStartController: RequestProductionAccessStartController
  )

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]
    val accessRequestSessionRepository = mock[AccessRequestSessionRepository]
    val applicationApiBuilder = mock[ApplicationApiBuilder]

    val playApplication = applicationBuilder(userAnswers = None, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[AccessRequestSessionRepository].toInstance(accessRequestSessionRepository),
        bind[ApplicationApiBuilder].toInstance(applicationApiBuilder),
        bind[Clock].toInstance(clock)
      )
      .build()

    val controller = playApplication.injector.instanceOf[RequestProductionAccessStartController]

    Fixture(playApplication, apiHubService, accessRequestSessionRepository, applicationApiBuilder, controller)
  }

}

object RequestProductionAccessStartControllerSpec {

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  private val apiDetail = ApiDetail(
    id = "test-id",
    publisherReference = "test-publisher-reference",
    title = "test-title",
    description = "test-description",
    version = "test-version",
    endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", None, None, Seq("test-scope"))))),
    shortDescription = None,
    openApiSpecification = "test-oas-spec",
    apiStatus = Live,
    reviewedDate = Instant.now(),
    platform = "HIP",
    maintainer = Maintainer("name", "#slack", List.empty)
  )

  private val application =
    FakeApplication
      .addApi(Api(apiDetail.id, apiDetail.title, Seq(SelectedEndpoint("GET", "/test"))))
      .setSecondaryScopes(Seq(Scope("test-scope")))

  private val applicationApi = ApplicationApi(
    apiDetail = apiDetail,
    endpoints = Seq.empty,
    hasPendingAccessRequest = false
  )

}
