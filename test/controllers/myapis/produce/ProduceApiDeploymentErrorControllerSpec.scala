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

package controllers.myapis.produce

import base.SpecBase
import config.Domains
import controllers.actions.FakeUser
import controllers.myapis.produce.routes as produceApiRoutes
import controllers.routes
import forms.myapis.produce.ProduceApiDomainFormProvider
import models.deployment.{Error, FailuresResponse}
import models.myapis.produce.ProduceApiDomainSubdomain
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.ProduceApiDeploymentErrorPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.myapis.produce.ProduceApiDeploymentErrorView

import scala.concurrent.Future

class ProduceApiDeploymentErrorControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private lazy val produceApiDeploymentErrorRoute = produceApiRoutes.ProduceApiDeploymentErrorController.onPageLoad().url

  "ProduceApiDeploymentError Controller" - {

    "must return OK and the correct view for a GET" in {

      val failuresResponse = FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      val userAnswers = emptyUserAnswers.set(ProduceApiDeploymentErrorPage, failuresResponse).get
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiDeploymentErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProduceApiDeploymentErrorView]
        val domains = application.injector.instanceOf[Domains]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(FakeUser, Some(failuresResponse))(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET and no answers" in {

      val failuresResponse = FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiDeploymentErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProduceApiDeploymentErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(FakeUser, None)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, produceApiDeploymentErrorRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
