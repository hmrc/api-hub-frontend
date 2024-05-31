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

package controllers.myapis

import base.SpecBase
import connectors.ApplicationsConnector
import controllers.actions.{FakeApiDetail, FakeSupporter}
import models.deployment.{Error, FailuresResponse, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.user.UserModel
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiPromotionView}

import scala.concurrent.Future

class SimpleApiPromotionControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with ArgumentMatchersSugar
    with HtmlValidation
    with TestHelpers {

  "onPageLoad" - {
    "must return 200 Ok and the correct view for a support user when the API exists" in {
      forAll(usersWhoCanSupport) {user =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApiDetail(any)(any)).thenReturn(Future.successful(Some(FakeApiDetail)))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onPageLoad(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[SimpleApiPromotionView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(FakeApiDetail, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService).getApiDetail(eqTo(FakeApiDetail.id))(any)
        }
      }
    }

    "must return 404 Not Found when the API does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.getApiDetail(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onPageLoad(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe view(
          "Page not found - 404",
          "API not found",
          s"Cannot find an API with Id ${FakeApiDetail.id}.")(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the unauthorised page for a non-support user" in {
      forAll(usersWhoCannotSupport) {user =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onPageLoad(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)

          verify(fixture.apiHubService, never).getApiDetail(any)(any)
        }
      }
    }
  }

  "onSubmit" - {
    "must return 200 Ok and the success page for a support user when successful" in {
      val response = SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      )

      forAll(usersWhoCanSupport) { user =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.getApiDetail(any)(any)).thenReturn(Future.successful(Some(FakeApiDetail)))
        when(fixture.applicationsConnector.promoteToProduction(any)(any)).thenReturn(Future.successful(Some(response)))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onSubmit(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[DeploymentSuccessView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(user, response)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService).getApiDetail(eqTo(FakeApiDetail.id))(any)
          verify(fixture.applicationsConnector).promoteToProduction(eqTo(FakeApiDetail.publisherReference))(any)
        }
      }
    }

    "must return 400 Bad Request and the failure page when APIM returns failure" in {
      val fixture = buildFixture(FakeSupporter)

      val response = InvalidOasResponse(
        failure = FailuresResponse(
          code = "test-code",
          reason = "test-reason",
          errors = Some(Seq(Error(`type` = "test-type", message = "test-message")))
        )
      )

      when(fixture.apiHubService.getApiDetail(any)(any)).thenReturn(Future.successful(Some(FakeApiDetail)))
      when(fixture.applicationsConnector.promoteToProduction(any)(any)).thenReturn(Future.successful(Some(response)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onSubmit(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[DeploymentFailureView]
        val returnUrl = controllers.myapis.routes.SimpleApiPromotionController.onPageLoad(FakeApiDetail.id).url

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(FakeSupporter, response.failure, returnUrl)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 404 Not Found when the API does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.getApiDetail(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onSubmit(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe view(
          "Page not found - 404",
          "API not found",
          s"Cannot find an API with Id ${FakeApiDetail.id}.")(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 404 Not Found when the API has not been deployed to APIM" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.getApiDetail(any)(any)).thenReturn(Future.successful(Some(FakeApiDetail)))
      when(fixture.applicationsConnector.promoteToProduction(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onSubmit(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe view(
          "Page not found - 404",
          "API not found",
          s"The API ${FakeApiDetail.title} has not been deployed to HIP.")(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the unauthorised page for a non-support user" in {
      forAll(usersWhoCannotSupport) { user =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.myapis.routes.SimpleApiPromotionController.onSubmit(FakeApiDetail.id))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)

          verify(fixture.apiHubService, never).getApiDetail(any)(any)
          verify(fixture.applicationsConnector, never).promoteToProduction(any)(any)
        }
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    applicationsConnector: ApplicationsConnector
  )

  private def buildFixture(user: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]
    val applicationsConnector = mock[ApplicationsConnector]

    val playApplication = applicationBuilder(user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ApplicationsConnector].toInstance(applicationsConnector)
      )
      .build()
    Fixture(playApplication, apiHubService, applicationsConnector)
  }

}
