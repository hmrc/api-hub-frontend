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
import controllers.actions.{FakeApiDetail, FakeUser}
import controllers.myapis.SimpleApiDeploymentController.DeploymentsRequestFormProvider
import fakes.{FakeDomains, FakeHods}
import models.application.TeamMember
import models.deployment.{DeploymentsRequest, EgressMapping, Error, FailuresResponse, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.team.Team
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.HtmlValidation
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiDeploymentView}

import java.time.LocalDateTime
import scala.concurrent.Future

class SimpleApiDeploymentControllerSpec
  extends SpecBase
    with MockitoSugar
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
        contentAsString(result) mustBe view(form, teams, FakeDomains, FakeHods, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiHubService).findTeams(eqTo(Some(FakeUser.email)))(any)
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
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(validForm*)
        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[DeploymentSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeUser, response.id, deploymentsRequest.name)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.applicationsConnector).generateDeployment(eqTo(deploymentsRequest))(any)
      }
    }

    "must bind an empty prefixesToRemove value correctly to an empty list" in {
      val fixture = buildFixture()

      val response = SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      )

      val form = validForm
        .filterNot(_._1.equals("prefixesToRemove"))
        .appended("prefixesToRemove" -> "")

      when(fixture.applicationsConnector.generateDeployment(any)(any)).thenReturn(Future.successful(response))
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(form*)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe OK

        val expected = deploymentsRequest.copy(prefixesToRemove = Seq.empty)
        verify(fixture.applicationsConnector).generateDeployment(eqTo(expected))(any)
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
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
          .withFormUrlEncodedBody(validForm*)
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
        "teamId",
        "oas",
        "status",
        "domain",
        "subdomain"
      )

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        forAll(fieldNames){fieldName =>
          val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
            .withFormUrlEncodedBody(invalidForm(fieldName)*)
          val result = route(fixture.playApplication, request).value

          val boundForm = bindForm(form, invalidForm(fieldName))
          val view = fixture.playApplication.injector.instanceOf[SimpleApiDeploymentView]

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(boundForm, teams, FakeDomains, FakeHods, FakeUser)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must validate egress prefix mappings correctly" in {
      val fixture = buildFixture()

      val prefixMappings = Table(
        ("value", "is valid"),
        ("", true),
        ("/prefix,/replacement", true),
        ("/prefix1,/replacement1\n/prefix2,/replacement2", true),
        ("/prefix/replacement", false),
        ("/prefix,/replacement,", false),
        ("/prefix1,/replacement1\n/prefix2,,/replacement2", false),
        ("/prefix1,/replacement1\n/prefix2/replacement2", false),
      )

      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))
      when(fixture.applicationsConnector.generateDeployment(any)(any)).thenReturn(Future.successful(SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      )))

      running(fixture.playApplication) {
        forAll(prefixMappings) { (egressPrefixMappings, isValid) =>
          val form: Seq[(String,String)] = validForm.filterNot(_._1.equals("egressMappings")) :+ "egressMappings" -> egressPrefixMappings
          val request = FakeRequest(controllers.myapis.routes.SimpleApiDeploymentController.onSubmit())
            .withFormUrlEncodedBody(form *)
          val result = route(fixture.playApplication, request).value

          if (isValid) {
            status(result) mustBe OK
          } else {
            status(result) mustBe BAD_REQUEST
          }
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

  val hod1 = "test-hod-1"
  val hod2 = "test-hod-2"
  val prefix1 = "test-prefix-1"
  val prefix2 = "test-prefix-2"
  val prefix3 = "test-prefix-3"
  val egress = "test-egress"
  val egressMappingPrefix1 = "test-egress-mapping-prefix-1"
  val egressMappingEgressPrefix1 = "test-egress-mapping-egress-prefix-1"
  val egressMappingPrefix2 = "test-egress-mapping-prefix-2"
  val egressMappingEgressPrefix2 = "test-egress-mapping-egress-prefix-2"

  val deploymentsRequest: DeploymentsRequest = DeploymentsRequest(
    lineOfBusiness = "test-line-of-business",
    name = "test-name",
    description = "test-description",
    egress = egress,
    teamId = "test-team-id",
    oas = "test-oas",
    passthrough = false,
    status = "test-status",
    domain = "test-domain",
    subDomain = "test-sub-domain",
    hods = Seq(hod1, hod2),
    prefixesToRemove = Seq(prefix1, prefix2, prefix3),
    egressMappings = Some(Seq(
      EgressMapping(egressMappingPrefix1, egressMappingEgressPrefix1),
      EgressMapping(egressMappingPrefix2, egressMappingEgressPrefix2)
    ))
  )

  val validForm: Seq[(String, String)] = Seq(
    "lineOfBusiness" -> deploymentsRequest.lineOfBusiness,
    "name" -> deploymentsRequest.name,
    "description" -> deploymentsRequest.description,
    "teamId" -> deploymentsRequest.teamId,
    "oas" -> deploymentsRequest.oas,
    "passthrough" -> deploymentsRequest.passthrough.toString,
    "status" -> deploymentsRequest.status,
    "domain" -> deploymentsRequest.domain,
    "subdomain" -> deploymentsRequest.subDomain,
    "hods[]" -> hod1,
    "hods[]" -> hod2,
    "prefixesToRemove" -> s"$prefix1 \n $prefix2  \r\n$prefix3",    // Deliberate mix of UNIX and Windows newlines with surplus whitespace
    "egress" -> egress,
    "egressMappings" -> s"  $egressMappingPrefix1,$egressMappingEgressPrefix1 \n $egressMappingPrefix2,$egressMappingEgressPrefix2 \r\n  "
  )

  def invalidForm(missingField: String): Seq[(String, String)] =
    validForm.filterNot(_._1.equalsIgnoreCase(missingField)) :+ (missingField, "")

  def bindForm(form: Form[?], values: Seq[(String, String)]): Form[?] = {
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
