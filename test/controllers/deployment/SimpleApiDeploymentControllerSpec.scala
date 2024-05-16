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

package controllers.deployment

import base.SpecBase
import connectors.ApplicationsConnector
import models.deployment.{Error, FailuresResponse, InvalidOasResponse, SuccessfulDeploymentsResponse}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService

import scala.concurrent.Future

class SimpleApiDeploymentControllerSpec extends SpecBase with MockitoSugar with ArgumentMatchersSugar {

  import SimpleApiDeploymentControllerSpec._

  "onSubmit" - {
    "must respond with 200 OK and a success JSON response when returned by APIM" in {
      val fixture = buildFixture()

      val response = SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      )

      when(fixture.applicationsConnector.generateDeployment(any)(any)).thenReturn(Future.successful(response))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.deployment.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(validForm: _*)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(response)
      }
    }

    "must return 400 Bad Request and a failure JSON response when returned by APIM" in {
      val fixture = buildFixture()

      val response = InvalidOasResponse(
        FailuresResponse(
          code = "BAD_REQUEST",
          reason = "Validation Failed.",
          errors = Some(Seq(Error("METADATA", """name must match \"^[a-z0-9\\-]+$\"""")))
        )
      )

      when(fixture.applicationsConnector.generateDeployment(any)(any)).thenReturn(Future.successful(response))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.deployment.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(validForm: _*)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(response)
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    applicationsConnector: ApplicationsConnector
  )

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]
    val applicationsConnector = mock[ApplicationsConnector]
    val playApplication = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ApplicationsConnector].toInstance(applicationsConnector)
      )
      .build()
    Fixture(playApplication, apiHubService, applicationsConnector)
  }

}

object SimpleApiDeploymentControllerSpec {

  val validForm = Seq(
    "lineOfBusiness" -> "test-line-of-business",
    "name" -> "test-name",
    "description" -> "test-description",
    "egress" -> "test-egress",
    "teamId" -> "test-team-id",
    "oas" -> "test-oas",
    "passthrough" -> "false",
    "status" -> "test-status"
  )

}
