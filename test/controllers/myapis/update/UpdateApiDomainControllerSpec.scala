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

package controllers.myapis.update

import base.SpecBase
import config.Domains
import controllers.actions.FakeUser
import controllers.myapis.update.routes as updateApiRoutes
import controllers.routes
import forms.myapis.produce.ProduceApiDomainFormProvider
import models.myapis.produce.ProduceApiDomainSubdomain
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.update.UpdateApiDomainPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UpdateApiSessionRepository
import viewmodels.myapis.produce.ProduceApiDomainViewModel
import views.html.myapis.produce.ProduceApiDomainView

import scala.concurrent.Future

class UpdateApiDomainControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val updateApiDomainRoute = updateApiRoutes.UpdateApiDomainController.onPageLoad(NormalMode).url

  def form(domains: Domains) = new ProduceApiDomainFormProvider(domains)()

  "UpdateApiDomainController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, updateApiDomainRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProduceApiDomainView]
        val viewModel = ProduceApiDomainViewModel("updateApiDomain.heading", updateApiRoutes.UpdateApiDomainController.onSubmit(NormalMode))
        val domains = application.injector.instanceOf[Domains]
        val boundForm = form(domains)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(boundForm, viewModel, FakeUser, domains)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(UpdateApiDomainPage, ProduceApiDomainSubdomain("domain", "subdomain")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, updateApiDomainRoute)

        val view = application.injector.instanceOf[ProduceApiDomainView]
        val viewModel = ProduceApiDomainViewModel("updateApiDomain.heading", updateApiRoutes.UpdateApiDomainController.onSubmit(NormalMode))
        val domains = application.injector.instanceOf[Domains]
        val boundForm = form(domains)
          .fill(ProduceApiDomainSubdomain("domain", "subdomain"))

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(boundForm, viewModel, FakeUser, domains)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UpdateApiSessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[UpdateApiSessionRepository].toInstance(mockSessionRepository),
          )
          .build()

      running(application) {
        val domains = application.injector.instanceOf[Domains]
        val domain = domains.domains.head
        val subDomain = domain.subDomains.head
        val request =
          FakeRequest(POST, updateApiDomainRoute)
            .withFormUrlEncodedBody(
              ("domain", domain.code),
              ("subDomain", subDomain.code)
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, updateApiDomainRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val domains = application.injector.instanceOf[Domains]
        val boundForm = form(domains).bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ProduceApiDomainView]
        val viewModel = ProduceApiDomainViewModel("updateApiDomain.heading", updateApiRoutes.UpdateApiDomainController.onSubmit(NormalMode))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModel, FakeUser, domains)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, updateApiDomainRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, updateApiDomainRoute)
            .withFormUrlEncodedBody(
              ("domain", "domain"),
              ("subDomain", "subDomain")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
