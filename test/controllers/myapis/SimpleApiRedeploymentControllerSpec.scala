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
import controllers.actions.{ApiAuthActionProvider, FakeApiAuthActions, FakeApiDetail, FakeUser}
import controllers.myapis.SimpleApiDeploymentControllerSpec.teams
import controllers.myapis.SimpleApiRedeploymentController.RedeploymentRequestFormProvider
import fakes.{FakeDomains, FakeHods}
import models.deployment.*
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.HtmlValidation
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiRedeploymentView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleApiRedeploymentControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with HtmlValidation
    with TableDrivenPropertyChecks
    with FakeApiAuthActions {

  import SimpleApiRedeploymentControllerSpec._

  "onPageLoad" - {
    "must return 200 Ok and the correct view" in {
      val fixture = buildFixture()

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
      when(fixture.apiHubService.getDeploymentDetails(any)(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[SimpleApiRedeploymentView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, FakeApiDetail, FakeDomains, FakeHods, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiAuthActionProvider).apply(eqTo(FakeApiDetail.id))(any)
        verify(fixture.apiHubService).getDeploymentDetails(eqTo(FakeApiDetail.publisherReference))(any)
      }
    }

    "must pre-populate fields when the data is available in APIM" in {
      val fixture = buildFixture()

      val deploymentDetails = DeploymentDetails(
        description = Some(redeploymentRequest.description),
        status = Some(redeploymentRequest.status),
        domain = Some(redeploymentRequest.domain),
        subDomain = Some(redeploymentRequest.subDomain),
        hods = Some(redeploymentRequest.hods),
        egressMappings = redeploymentRequest.egressMappings,
        prefixesToRemove = Some(redeploymentRequest.prefixesToRemove)
      )

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
      when(fixture.apiHubService.getDeploymentDetails(any)(any)).thenReturn(Future.successful(Some(deploymentDetails)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[SimpleApiRedeploymentView]

        val filledForm = form.fill(redeploymentRequest.copy(oas = ""))

        status(result) mustBe OK
        contentAsString(result) mustBe view(filledForm, FakeApiDetail, FakeDomains, FakeHods, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
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

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
      when(fixture.applicationsConnector.updateDeployment(any, any)(any)).thenReturn(Future.successful(Some(response)))
      when(fixture.apiHubService.findTeams(any)(any)).thenReturn(Future.successful(teams))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
          .withFormUrlEncodedBody(validForm*)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[DeploymentSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeUser, response)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiAuthActionProvider).apply(eqTo(FakeApiDetail.id))(any)
        verify(fixture.applicationsConnector).updateDeployment(eqTo(FakeApiDetail.publisherReference), eqTo(redeploymentRequest))(any)
      }
    }

    "must respond with 400 Bad Request and a failure view response when failure returned by APIM" in {
      val fixture = buildFixture()

      val response = InvalidOasResponse(
        failure = FailuresResponse(
          code = "test-code",
          reason = "test-reason",
          errors = Some(Seq(Error(`type` = "test-type", message = "test-message")))
        )
      )

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))
      when(fixture.applicationsConnector.updateDeployment(any, any)(any)).thenReturn(Future.successful(Some(response)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
          .withFormUrlEncodedBody(validForm*)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[DeploymentFailureView]
        val returnUrl = controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(FakeApiDetail.id).url

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(FakeUser, response.failure, returnUrl)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 400 Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture()

      val fieldNames = Table(
        "Field name",
        "description",
        "oas",
        "status",
        "domain",
        "subdomain"
      )

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))

      running(fixture.playApplication) {
        forAll(fieldNames){(fieldName: String) =>
          val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
            .withFormUrlEncodedBody(invalidForm(fieldName)*)
          val result = route(fixture.playApplication, request).value
          val boundForm = bindForm(form, invalidForm(fieldName))

          val view = fixture.playApplication.injector.instanceOf[SimpleApiRedeploymentView]

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(boundForm, FakeApiDetail, FakeDomains, FakeHods, FakeUser)(request, messages(fixture.playApplication)).toString()
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

      when(fixture.applicationsConnector.updateDeployment(any, any)(any)).thenReturn(Future.successful(Some(SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      ))))
      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))


      running(fixture.playApplication) {
        forAll(prefixMappings) { (egressPrefixMappings, isValid) =>
          val form: Seq[(String,String)] = validForm.filterNot(_._1.equals("egressMappings")) :+ "egressMappings" -> egressPrefixMappings
          val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
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
    applicationsConnector: ApplicationsConnector,
    apiAuthActionProvider: ApiAuthActionProvider
  )

  private def buildFixture(user: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]
    val applicationsConnector = mock[ApplicationsConnector]
    val apiAuthActionProvider = mock[ApiAuthActionProvider]

    val playApplication = applicationBuilder(user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ApplicationsConnector].toInstance(applicationsConnector),
        bind[ApiAuthActionProvider].toInstance(apiAuthActionProvider)
      )
      .build()
    Fixture(playApplication, apiHubService, applicationsConnector, apiAuthActionProvider)
  }

}

object SimpleApiRedeploymentControllerSpec extends OptionValues {

  private val form = new RedeploymentRequestFormProvider()()

  val hod1 = "test-hod-1"
  val hod2 = "test-hod-2"
  val prefix1 = "test-prefix-1"
  val prefix2 = "test-prefix-2"
  val prefix3 = "test-prefix-3"
  val egressMappingPrefix1 = "test-egress-mapping-prefix-1"
  val egressMappingEgressPrefix1 = "test-egress-mapping-egress-prefix-1"
  val egressMappingPrefix2 = "test-egress-mapping-prefix-2"
  val egressMappingEgressPrefix2 = "test-egress-mapping-egress-prefix-2"

  private val redeploymentRequest = RedeploymentRequest(
    description = "test-description",
    oas = "test-oas",
    status = "test-status",
    domain = "1",
    subDomain = "1.1",
    hods = Seq(hod1, hod2),
    prefixesToRemove = Seq(prefix1, prefix2, prefix3),
    egressMappings = Some(Seq(
      EgressMapping(egressMappingPrefix1, egressMappingEgressPrefix1),
      EgressMapping(egressMappingPrefix2, egressMappingEgressPrefix2)
    ))
  )

  private val validForm = Seq(
    "description" -> redeploymentRequest.description,
    "oas" -> redeploymentRequest.oas,
    "status" -> redeploymentRequest.status,
    "domain" -> redeploymentRequest.domain,
    "subdomain" -> redeploymentRequest.subDomain,
    "hods[]" -> hod1,
    "hods[]" -> hod2,
    "prefixesToRemove" -> s"$prefix1 \n $prefix2  \r\n$prefix3",    // Deliberate mix of UNIX and Windows newlines with surplus whitespace
    "egressMappings" -> s"  $egressMappingPrefix1,$egressMappingEgressPrefix1 \n $egressMappingPrefix2,$egressMappingEgressPrefix2 \r\n  "
  )

  private def invalidForm(missingField: String): Seq[(String, String)] =
    validForm.filterNot(_._1.equalsIgnoreCase(missingField)) :+ (missingField, "")

  private def bindForm(form: Form[?], values: Seq[(String, String)]): Form[?] = {
    form.bind(values.toMap)
  }

}
