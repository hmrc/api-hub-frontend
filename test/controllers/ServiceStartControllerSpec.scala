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

package controllers

import base.OptionallyAuthenticatedSpecBase
import config.FrontendAppConfig
import models.stats.DashboardStatistics
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.ServiceStartViewModel
import views.html.ServiceStartView

import scala.concurrent.Future

class ServiceStartControllerSpec extends OptionallyAuthenticatedSpecBase with HtmlValidation with OptionValues with MockitoSugar {

  import ServiceStartControllerSpec.*

  "Service Start Controller" - {
    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture()
      val dashboardStatistics = DashboardStatistics(23, 12)

      when(fixture.apiHubService.fetchDashboardStatistics()(any))
        .thenReturn(Future.successful(dashboardStatistics))

      running(fixture.application) {
        val request = FakeRequest(routes.ServiceStartController.onPageLoad)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ServiceStartView]
        val frontendAppConfig = fixture.application.injector.instanceOf[FrontendAppConfig]

        val viewModel = ServiceStartViewModel(
          user = None,
          dashboardStatistics = dashboardStatistics,
          documentationLinks = frontendAppConfig.startPageLinks
        )

        status(result) mustBe OK
        contentAsString(result) mustBe view(viewModel)(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(None)
      .overrides(bind[ApiHubService].toInstance(apiHubService))
      .build()

    Fixture(application, apiHubService)
  }

}

private object ServiceStartControllerSpec {

  case class Fixture(
    application: Application,
    apiHubService: ApiHubService
  )

}
