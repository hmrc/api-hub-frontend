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

package controllers.admin

import base.SpecBase
import controllers.actions.FakeApprover
import controllers.routes
import generators.AccessRequestGenerator
import models.accessrequest.{AccessRequest, Pending}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.AccessRequestsView

import java.time.LocalDateTime
import scala.concurrent.Future

class AccessRequestsControllerSpec
  extends SpecBase with MockitoSugar with HtmlValidation with TestHelpers with AccessRequestGenerator {

  "AccessRequestsController" - {
    "must return Ok and the correct view for an approver or support" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)

        val accessRequests = Seq(sampleAccessRequest())
        when(fixture.apiHubService.getAccessRequests(any(), any())(any())).thenReturn(Future.successful(accessRequests))

        running(fixture.playApplication) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, controllers.admin.routes.AccessRequestsController.onPageLoad().url)
          implicit val msgs: Messages = messages(fixture.playApplication)
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[AccessRequestsView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(accessRequests, user).toString()
          contentAsString(result) must validateAsHtml
          contentAsString(result) must include(s"API production access requests (<span id=\"requestCount\">${accessRequests.size}</span>)")
          verify(fixture.apiHubService).getAccessRequests(eqTo(None), eqTo(None))(any())
        }
      }
    }

    "must sort the access requests correctly" in {
      val now = LocalDateTime.now()

      // Access requests defined in the order they should appear
      val accessRequest1 = AccessRequest(
        id = "test-access-request-id-1",
        applicationId = "test-application-id-1",
        apiId = "test-api-id-1",
        apiName = "test-api-name-1",
        status = Pending,
        supportingInformation = "test-supporting-information-1",
        requested = now,
        requestedBy = "test-requested-by-1",
        environmentId = "test"
      )

      val accessRequest2 = AccessRequest(
        id = "test-access-request-id-2",
        applicationId = "test-application-id-2",
        apiId = "test-api-id-2",
        apiName = "test-api-name-2",
        status = Pending,
        supportingInformation = "test-supporting-information-2",
        requested = now.minusDays(1),
        requestedBy = "test-requested-by-2",
        environmentId = "test"
      )

      val accessRequest3 = AccessRequest(
        id = "test-access-request-id-3",
        applicationId = "test-application-id-2",
        apiId = "test-api-id-3",
        apiName = "test-api-name-3",
        status = Pending,
        supportingInformation = "test-supporting-information-3",
        requested = now.minusDays(1),
        requestedBy = "test-requested-by-3",
        environmentId = "test"
      )

      val accessRequest4 = AccessRequest(
        id = "test-access-request-id-4",
        applicationId = "test-application-id-4",
        apiId = "test-api-id-4",
        apiName = "test-api-name-4",
        status = Pending,
        supportingInformation = "test-supporting-information-4",
        requested = now.minusDays(2),
        requestedBy = "test-requested-by-4",
        environmentId = "test"
      )

      val accessRequests = Seq(accessRequest4, accessRequest3, accessRequest2, accessRequest1)
      val orderedAccessRequests = Seq(accessRequest1, accessRequest2, accessRequest3, accessRequest4)

      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.getAccessRequests(any(), any())(any())).thenReturn(Future.successful(accessRequests))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, controllers.admin.routes.AccessRequestsController.onPageLoad().url)
        implicit val msgs: Messages = messages(fixture.playApplication)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[AccessRequestsView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(orderedAccessRequests, FakeApprover).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the unauthorised page for a user who is not an approver or support" in {
      forAll(usersWhoCannotViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(GET, controllers.admin.routes.AccessRequestsController.onPageLoad().url)
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
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
