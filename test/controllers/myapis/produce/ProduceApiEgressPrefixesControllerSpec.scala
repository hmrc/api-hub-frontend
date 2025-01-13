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
import config.FrontendAppConfig
import controllers.actions.FakeUser
import controllers.routes
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import forms.myapis.produce.ProduceApiEgressPrefixesFormProvider
import pages.myapis.produce.ProduceApiEgressPrefixesPage
import models.myapis.produce.ProduceApiEgressPrefixes
import viewmodels.myapis.produce.ProduceApiEgressPrefixesViewModel

import scala.concurrent.Future
import views.html.myapis.produce.ProduceApiEgressPrefixesView

class ProduceApiEgressPrefixesControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ProduceApiEgressPrefixesFormProvider()
  val form = formProvider()
  val user = FakeUser

  lazy val produceApiEgressPrefixesRoute = controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(NormalMode).url

  val userAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      ProduceApiEgressPrefixesPage.toString -> Json.obj(
        "prefixes" -> Seq("/prefix"),
        "mappings" -> Seq("/a->/b")
      )
    )
  )

  "ProduceApiEgressPrefixes Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiEgressPrefixesRoute)

        val view = application.injector.instanceOf[ProduceApiEgressPrefixesView]
        val viewModel = ProduceApiEgressPrefixesViewModel("produceApiEgressPrefix.heading", controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onSubmit(NormalMode))
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel, user, config.helpDocsPath)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, produceApiEgressPrefixesRoute)

        val view = application.injector.instanceOf[ProduceApiEgressPrefixesView]
        val viewModel = ProduceApiEgressPrefixesViewModel("produceApiEgressPrefix.heading", controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onSubmit(NormalMode))
        val config = application.injector.instanceOf[FrontendAppConfig]
        val model = ProduceApiEgressPrefixes(Seq("/prefix"), Seq("/a->/b"))
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(model), viewModel, user, config.helpDocsPath)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiEgressPrefixesRoute)
            .withFormUrlEncodedBody(("prefixes", "value 1"), ("mappings", "value 2"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiEgressPrefixesRoute)
            .withFormUrlEncodedBody(("prefixes[]", "asdf"))

        val boundForm = form.bindFromRequest(Map("prefixes[]" -> Seq("asdf")))

        val view = application.injector.instanceOf[ProduceApiEgressPrefixesView]
        val viewModel = ProduceApiEgressPrefixesViewModel("produceApiEgressPrefix.heading", controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onSubmit(NormalMode))
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModel, user, config.helpDocsPath)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, produceApiEgressPrefixesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, produceApiEgressPrefixesRoute)
            .withFormUrlEncodedBody(("prefixes", "value 1"), ("mappings", "value 2"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
