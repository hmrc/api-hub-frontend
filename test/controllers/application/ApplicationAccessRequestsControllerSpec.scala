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
import controllers.actions.{FakeApplication, FakeSupporter}
import controllers.routes
import generators.AccessRequestGenerator
import models.accessrequest.{AccessRequest, Pending}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.application.ApplicationAccessRequestsView

import java.time.LocalDateTime
import scala.concurrent.Future

class ApplicationAccessRequestsControllerSpec
  extends SpecBase with MockitoSugar with HtmlValidation with TestHelpers with AccessRequestGenerator {

  "ApplicationAccessRequestsController" - {
    "must return Ok and the correct view for a permitted user" in {
      forAll(teamMemberAndSupporterTable) { (user: UserModel) =>
        val fixture = buildFixture(user)

        val accessRequests = Seq(sampleAccessRequest())
        when(fixture.apiHubService.getAccessRequests(eqTo(Some(FakeApplication.id)), any())(any())).thenReturn(Future.successful(accessRequests))
        when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any())(any())).thenReturn(Future.successful(Some(FakeApplication)))

        running(fixture.playApplication) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(FakeApplication.id).url)
          implicit val msgs: Messages = messages(fixture.playApplication)
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[ApplicationAccessRequestsView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(FakeApplication, accessRequests, user).toString()
          contentAsString(result) must validateAsHtml
          verify(fixture.apiHubService).getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(None))(any())
        }
      }
    }

    "must sort the access requests correctly" in {
      val now = LocalDateTime.now()

      // Access requests defined in the order they should appear
      val accessRequest1 = AccessRequest(
        id = "test-access-request-id-1",
        applicationId = FakeApplication.id,
        apiId = "test-api-id-1",
        apiName = "test-api-name-1",
        status = Pending,
        supportingInformation = "test-supporting-information-1",
        requested = now,
        requestedBy = "test-requested-by-1"
      )

      val accessRequest2 = AccessRequest(
        id = "test-access-request-id-2",
        applicationId = FakeApplication.id,
        apiId = "test-api-id-2",
        apiName = "test-api-name-2",
        status = Pending,
        supportingInformation = "test-supporting-information-2",
        requested = now.minusDays(1),
        requestedBy = "test-requested-by-2"
      )

      val accessRequest3 = AccessRequest(
        id = "test-access-request-id-3",
        applicationId = FakeApplication.id,
        apiId = "test-api-id-3",
        apiName = "test-api-name-3",
        status = Pending,
        supportingInformation = "test-supporting-information-3",
        requested = now.minusDays(1),
        requestedBy = "test-requested-by-3"
      )

      val accessRequest4 = AccessRequest(
        id = "test-access-request-id-4",
        applicationId = FakeApplication.id,
        apiId = "test-api-id-4",
        apiName = "test-api-name-4",
        status = Pending,
        supportingInformation = "test-supporting-information-4",
        requested = now.minusDays(2),
        requestedBy = "test-requested-by-4"
      )

      val accessRequests = Seq(accessRequest4, accessRequest3, accessRequest2, accessRequest1)
      val orderedAccessRequests = Seq(accessRequest1, accessRequest2, accessRequest3, accessRequest4)

      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.getAccessRequests(any(), any())(any())).thenReturn(Future.successful(accessRequests))
      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), any())(any())).thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, controllers.application.routes.ApplicationAccessRequestsController.onPageLoad(FakeApplication.id).url)
        implicit val msgs: Messages = messages(fixture.playApplication)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ApplicationAccessRequestsView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeApplication, orderedAccessRequests, FakeSupporter).toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
