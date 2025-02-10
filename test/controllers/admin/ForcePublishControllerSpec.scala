/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.actions.FakeSupporter
import fakes.FakeHipEnvironments
import forms.admin.ForcePublishPublisherReferenceFormProvider
import generators.ApiDetailGenerators
import models.api.ApiDeploymentStatus.Deployed
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.admin.ForcePublishViewModel
import views.html.ErrorTemplate
import views.html.admin.{ForcePublishSuccessView, ForcePublishView}

import scala.concurrent.Future

class ForcePublishControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation
    with ApiDetailGenerators {

  import ForcePublishControllerSpec.*

  "onPageLoad" - {
    "must return Ok and the correct view for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.onPageLoad())
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ForcePublishView]

          val viewModel = ForcePublishViewModel(
            form = form,
            user = user
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(viewModel)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.onPageLoad())
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  "onSubmit" - {
    "must redirect back to the force publish view when valid data is entered by a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.onSubmit())
            .withFormUrlEncodedBody(("value", publisherReference))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ForcePublishController.showVersionComparison(publisherReference).url)
        }
      }
    }

    "must return Bad Request and errors when invalid data is submitted by a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.onSubmit())
            .withFormUrlEncodedBody(("value", ""))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ForcePublishView]
          val formWithErrors = form.bind(Map("value" -> ""))

          val viewModel = ForcePublishViewModel(
            form = formWithErrors,
            user = user
          )

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(viewModel)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.onSubmit())
            .withFormUrlEncodedBody(("value", publisherReference))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  "showVersionComparison" - {
    "must return Ok and the correct view for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val environment = FakeHipEnvironments.deploymentHipEnvironment
        val deploymentStatus = Deployed(environment.id, "test-deployed-version")
        val apiDetail = sampleApiDetail()

        when(fixture.apiHubService.getApiDeploymentStatus(
          eqTo(FakeHipEnvironments.deploymentHipEnvironment),
          eqTo(publisherReference)
        )(any)).thenReturn(Future.successful(deploymentStatus))

        when(fixture.apiHubService.getApiDetailForPublishReference(eqTo(publisherReference))(any))
          .thenReturn(Future.successful(Some(apiDetail)))

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.showVersionComparison(publisherReference))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ForcePublishView]

          val viewModel = ForcePublishViewModel(
            form = form.fill(publisherReference),
            user = user,
            publisherReference = Some(publisherReference),
            deploymentStatus = Some(deploymentStatus),
            catalogueVersion = Some(apiDetail.version)
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(viewModel)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.showVersionComparison(publisherReference))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  "forcePublish" - {
    "must force publish the API and return the success view for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.forcePublish(eqTo(publisherReference))(any))
          .thenReturn(Future.successful(Some(())))

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.forcePublish(publisherReference))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ForcePublishSuccessView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(publisherReference, user)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return 404 Not Found when the deployment is not found in APIM" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.forcePublish(eqTo(publisherReference))(any))
          .thenReturn(Future.successful(None))

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.forcePublish(publisherReference))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ErrorTemplate]

          status(result) mustBe NOT_FOUND

          contentAsString(result) mustBe
            view(
              "Page not found - 404",
              "This deployment was not found in APIM",
              s"Could not find a deployment or OAS document in APIM for publisher reference $publisherReference.",
              Some(user)
            )(request, messages(fixture.application))
              .toString()

          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.ForcePublishController.forcePublish(publisherReference))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  private def buildFixture(user: UserModel = FakeSupporter): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(None, user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(apiHubService, application)
  }

}

private object ForcePublishControllerSpec {

  val form: Form[String] = new ForcePublishPublisherReferenceFormProvider()()
  val publisherReference = "test-publisher-reference"

  case class Fixture(
    apiHubService: ApiHubService,
    application: Application
  )

}
