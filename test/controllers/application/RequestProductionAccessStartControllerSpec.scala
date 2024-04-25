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
import generators.ApiDetailGenerators
import models.UserAnswers
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live}
import models.application.ApplicationLenses.ApplicationLensOps
import models.application._
import models.user.UserModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AccessRequestApplicationIdPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import utils.HtmlValidation

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class RequestProductionAccessStartControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  "RequestProductionAccessStartController" - {
    "must initiate user answers with the application and persist this in the session repository" in {
      val fixture = buildFixture()
      val application: Application = anApplication

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.accessRequestSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.RequestProductionAccessStartController.onPageLoad(application.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER

        val expected = UserAnswers(id = FakeUser.userId, lastUpdated = clock.instant())
          .set(AccessRequestApplicationIdPage, application).toOption.value

        verify(fixture.accessRequestSessionRepository).set(ArgumentMatchers.eq(expected))
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture()
      val application: Application = anApplication

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), any())(any()))
        .thenReturn(Future.successful(Some(application)))


      when(fixture.accessRequestSessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.RequestProductionAccessStartController.onPageLoad(application.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RequestProductionAccessController.onPageLoad().url)
      }
    }

    "must redirect to unauthorised" in {
      val fixture = buildFixture(FakeUserNotTeamMember)
      val application: Application = FakeApplication

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), any())(any()))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.RequestProductionAccessStartController.onPageLoad(application.id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  private def anApplication = {
    val apiDetail = ApiDetail(
      id = "test-id",
      publisherReference = "test-publisher-reference",
      title = "test-title",
      description = "test-description",
      version = "test-version",
      endpoints = Seq(Endpoint(path = "/test", methods = Seq(EndpointMethod("GET", None, None, Seq("test-scope"))))),
      shortDescription = None,
      openApiSpecification = "test-oas-spec",
      apiStatus = Live
    )
    val application = FakeApplication
      .addApi(Api(apiDetail.id, Seq(SelectedEndpoint("GET", "/test"))))
      .setSecondaryScopes(Seq(Scope("test-scope")))

    application
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              accessRequestSessionRepository: AccessRequestSessionRepository,
                              requestProductionAccessStartController: RequestProductionAccessStartController
                            )

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]
    val accessRequestSessionRepository = mock[AccessRequestSessionRepository]

    val application = applicationBuilder(userAnswers = None, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[AccessRequestSessionRepository].toInstance(accessRequestSessionRepository),
        bind[Clock].toInstance(clock)
      )
      .build()

    val controller = application.injector.instanceOf[RequestProductionAccessStartController]
    Fixture(application, apiHubService, accessRequestSessionRepository, controller)
  }

}
