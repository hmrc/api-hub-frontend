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
import controllers.actions.FakeUser
import controllers.myapis.SimpleApiDeploymentController.DeploymentsRequestFormProvider
import models.application.TeamMember
import models.deployment.{Error, FailuresResponse, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.team.Team
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.HtmlValidation
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiDeploymentView}

import java.time.LocalDateTime
import scala.concurrent.Future

class SimpleApiDeploymentControllerSpec
  extends SpecBase
    with MockitoSugar
    with ArgumentMatchersSugar
    with HtmlValidation
    with TableDrivenPropertyChecks {

  import SimpleApiDeploymentControllerSpec._

  "onPageLoad" - {
    "must return 200 OK and the correct view" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onPageLoad())
        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[SimpleApiDeploymentView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, teams, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeams(eqTo(Some(FakeUser.email.value)))(any)
      }
    }
  }

  "onSubmit" - {
    "must respond with 200 OK and a success view response when success returned by APIM" in {
      val fixture = buildFixture()

      val response = SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      )

      when(fixture.applicationsConnector.generateDeployment(any)(any)).thenReturn(Future.successful(response))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(validForm: _*)
        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[DeploymentSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeUser, response)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

      }
    }

    "must return 400 Bad Request and a failure view response when errors returned by APIM" in {
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
        val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(validForm: _*)
        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[DeploymentFailureView]

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(FakeUser, response.failure, controllers.myapis.routes.SimpleApiDeploymentController.onPageLoad().url)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

      }
    }

    "must return 400 Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture()

      val fieldNames = Table(
        "Field name",
        "lineOfBusiness",
        "name",
        "description",
        "egress",
        "teamId",
        "oas",
        "status"
      )

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        forAll(fieldNames){fieldName =>
          val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
            .withFormUrlEncodedBody(invalidForm(fieldName): _*)
          val result = route(fixture.playApplication, request).value

          val boundForm = bindForm(form, invalidForm(fieldName))
          val view = fixture.playApplication.injector.instanceOf[SimpleApiDeploymentView]

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(boundForm, teams, FakeUser)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
        }
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

  val form = new DeploymentsRequestFormProvider()()

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

  def invalidForm(missingField: String): Seq[(String, String)] =
    validForm.filterNot(_._1.equalsIgnoreCase(missingField)) :+ (missingField, "")

  def bindForm(form: Form[_], values: Seq[(String, String)]): Form[_] = {
    form.bind(values.toMap)
  }

  val team1: Team = Team(
    id = "test-id-1",
    name = "test-name-1",
    created = LocalDateTime.now(),
    teamMembers = Seq(
      TeamMember("test-email")
    )
  )

  val team2: Team = Team(
    id = "test-id-2",
    name = "test-name-2",
    created = LocalDateTime.now(),
    teamMembers = Seq(
      TeamMember("test-email")
    )
  )

  val teams: Seq[Team] = Seq(team1, team2)

}
