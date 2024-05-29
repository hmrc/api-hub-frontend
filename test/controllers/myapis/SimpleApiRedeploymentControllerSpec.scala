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
import controllers.myapis.SimpleApiRedeploymentController.RedeploymentRequestFormProvider
import models.deployment._
import models.user.UserModel
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.HtmlValidation
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiRedeploymentView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleApiRedeploymentControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with ArgumentMatchersSugar
    with HtmlValidation
    with TableDrivenPropertyChecks
    with FakeApiAuthActions {

  import SimpleApiRedeploymentControllerSpec._

  "onPageLoad" - {
    "must return 200 Ok and the correct view" in {
      val fixture = buildFixture()

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[SimpleApiRedeploymentView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, FakeApiDetail, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.apiAuthActionProvider).apply(eqTo(FakeApiDetail.id))(any)
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

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
          .withFormUrlEncodedBody(validForm: _*)
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
          .withFormUrlEncodedBody(validForm: _*)
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
        "status"
      )

      when(fixture.apiAuthActionProvider.apply(any)(any)).thenReturn(successfulApiAuthAction(FakeApiDetail))

      running(fixture.playApplication) {
        forAll(fieldNames){(fieldName: String) =>
          val request = FakeRequest(controllers.myapis.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
            .withFormUrlEncodedBody(invalidForm(fieldName): _*)
          val result = route(fixture.playApplication, request).value
          val boundForm = bindForm(form, invalidForm(fieldName))

          val view = fixture.playApplication.injector.instanceOf[SimpleApiRedeploymentView]

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(boundForm, FakeApiDetail, FakeUser)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
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

  private val redeploymentRequest = RedeploymentRequest(
    description = "test-description",
    oas = "test-oas",
    status = "test-status"
  )

  private val validForm = Seq(
    "description" -> redeploymentRequest.description,
    "oas" -> redeploymentRequest.oas,
    "status" -> redeploymentRequest.status
  )

  private def invalidForm(missingField: String): Seq[(String, String)] =
    validForm.filterNot(_._1.equalsIgnoreCase(missingField)) :+ (missingField, "")

  private def bindForm(form: Form[_], values: Seq[(String, String)]): Form[_] = {
    form.bind(values.toMap)
  }

}
