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
import fakes.FakeHubStatusService
import forms.admin.FeatureStatusChangeFormProvider
import models.hubstatus.{FeatureStatus, FrontendShutter}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.FormError
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{ApiHubService, HubStatusService}
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.{ShutterSuccessView, ShutterView}

import scala.concurrent.Future

class ShutterControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation {

  import ShutterControllerSpec.*

  "onPageLoad" - {
    "must return Ok and the correct view for a support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.ShutterController.onPageLoad())
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ShutterView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(form, FakeHubStatusService.frontendShutterStatus, user)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.ShutterController.onPageLoad())
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  "onSubmit" - {
    "must return Ok and the success view for a support user when valid data is submitted " in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.ShutterController.onSubmit())
            .withFormUrlEncodedBody(("shuttered", "false"))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ShutterSuccessView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(FakeHubStatusService.frontendShutterStatus, user)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.ShutterController.onSubmit())
            .withFormUrlEncodedBody(("shuttered", "false"))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must return 404 Bad Request and errors when no selection is made" in {
      val fixture = buildFixture(FakeSupporter)

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.routes.ShutterController.onSubmit())
          .withFormUrlEncodedBody(("shuttered", ""))
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ShutterView]
        val formWithErrors = form.bind(Map("shuttered" -> ""))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(formWithErrors, FakeHubStatusService.frontendShutterStatus, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 404 Bad Request and errors when the user shutters without a shutter message" in {
      val fixture = buildFixture(FakeSupporter)

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.routes.ShutterController.onSubmit())
          .withFormUrlEncodedBody(("shuttered", "true"), ("shutterMessage", ""))
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ShutterView]
        val formWithErrors = form
          .bind(Map("shuttered" -> "true", "shutterMessage" -> ""))
          .withError(FormError("shutterMessage", "shutter.shutterMessage.error.required"))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(formWithErrors, FakeHubStatusService.frontendShutterStatus, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private def buildFixture(user: UserModel): Fixture = {
    val hubStatusService = mock[HubStatusService]

    val application = applicationBuilder(None, user)
      .overrides(
      ).build()

    Fixture(application)
  }

}

private object ShutterControllerSpec {

  val form = new FeatureStatusChangeFormProvider()()

  case class Fixture(application: Application)

}
