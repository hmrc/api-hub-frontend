/*
 * Copyright 2024 HM Revenue & Customs
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
import fakes.FakeHipEnvironments
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Application as PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.admin.{ApimRequests, TestApimEndpointsViewModel}
import views.html.admin.TestApimEndpointsView

import scala.concurrent.Future

class TestApimEndpointsControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation {

  "TestApimEndpointsController" - {
    "onPageLoad" - {
      "must return Ok and the correct view for a support user" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)
  
          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.onPageLoad())
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[TestApimEndpointsView]
            implicit val msgs: Messages = messages(fixture.application)
            val viewModel = TestApimEndpointsViewModel(FakeHipEnvironments)

            status(result) mustBe OK
            contentAsString(result) mustBe view(viewModel, user)(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
        }
      }
  
      "must return Unauthorized for a non-support user" in {
        forAll(usersWhoCannotSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.onPageLoad())
            val result = route(fixture.application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
        }
      }
    }

    "callApim" - {
      "must return Ok with an APIM response for a support user" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)
          val apimResponse = "APIM says Hi"
          when(fixture.apiHubService.testApimEndpoint(any(), any(), any())(any())).thenReturn(Future.successful(Right(apimResponse)))

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.callApim(FakeHipEnvironments.test.id, ApimRequests.listEgressGateways.id, "p1,p2,,p4"))
            val result = route(fixture.application, request).value

            status(result) mustBe OK
            contentAsString(result) mustBe apimResponse
            verify(fixture.apiHubService).testApimEndpoint(eqTo(FakeHipEnvironments.test), eqTo(ApimRequests.listEgressGateways), eqTo(Seq("p1", "p2", "p4")))(any())
          }
        }
      }

      "must return Bad Request for unrecognised hip environment" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.callApim("not an env", ApimRequests.listEgressGateways.id, "param123"))
            val result = route(fixture.application, request).value

            status(result) mustBe BAD_REQUEST
          }
        }
      }

      "must return Bad Request for unrecognised endpoint id" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.callApim(FakeHipEnvironments.test.id, "not an endpoint", "param123"))
            val result = route(fixture.application, request).value

            status(result) mustBe BAD_REQUEST
          }
        }
      }

      "must return Bad Request with error details if APIM returns an error" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)
          val apimError = "Noooo!"
          when(fixture.apiHubService.testApimEndpoint(any(), any(), any())(any())).thenReturn(Future.successful(Left(new IllegalArgumentException(apimError))))

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.callApim(FakeHipEnvironments.test.id, ApimRequests.listEgressGateways.id, "param123"))
            val result = route(fixture.application, request).value

            status(result) mustBe BAD_REQUEST
            contentAsString(result) mustBe apimError
          }
        }
      }

      "must return Unauthorized for a non-support user" in {
        forAll(usersWhoCannotSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.TestApimEndpointsController.callApim("env", "enmdpointId", "param123"))
            val result = route(fixture.application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
        }
      }
    }
  }

  private case class Fixture(application: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
