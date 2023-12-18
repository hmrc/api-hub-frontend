/*
 * Copyright 2023 HM Revenue & Customs
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

import base.SpecBase
import controllers.actions.FakeUser
import generators.ApiDetailGenerators
import models.api.ApiDetail
import models.application.Application
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.{AddAnApiSuccessView, ApplicationDeletedSuccessView, ErrorTemplate}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class DeleteApplicationSuccessControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {


  "GET" - {

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApplicationDeletedSuccessView]

        val request = FakeRequest(GET, routes.DeleteApplicationSuccessController.onPageLoad.url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(Some(FakeUser))(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

  }

  private case class Fixture(
                              application: PlayApplication,
                              deleteApplicationSuccessController: DeleteApplicationSuccessController
                            )

  private def buildFixture: Fixture = {
    val apiHubService = mock[ApiHubService]
    val addAnApiSessionRepository = mock[AddAnApiSessionRepository]

    val application = applicationBuilder(None)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    val controller = application.injector.instanceOf[DeleteApplicationSuccessController]
    Fixture(application, controller)
  }

}