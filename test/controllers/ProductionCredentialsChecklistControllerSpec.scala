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
import controllers.actions.{FakeApplication, FakeUser}
import forms.ProductionCredentialsChecklistFormProvider
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.{Configuration, Application => PlayApplication}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.ProductionCredentialsChecklistView

import scala.concurrent.Future

class ProductionCredentialsChecklistControllerSpec extends SpecBase with MockitoSugar {

  import ProductionCredentialsChecklistControllerSpec._

  private val formProvider = new ProductionCredentialsChecklistFormProvider()
  private val form = formProvider()

  private lazy val productionCredentialsChecklistRoute = routes.ProductionCredentialsChecklistController.onPageLoad(FakeApplication.id).url

  "ProductionCredentialsChecklist Controller" - {

    "must return OK and the correct view for a GET" in {

      val fixture = buildFixture()

      running(fixture.playApplication) {
        val request = FakeRequest(GET, productionCredentialsChecklistRoute)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[ProductionCredentialsChecklistView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, FakeApplication.id)(request, messages(fixture.playApplication)).toString
      }
    }

    "must redirect to the Generate Primary Secret Success page when valid data is submitted" in {
      val fixture = buildFixture()

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, productionCredentialsChecklistRoute)
            .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GeneratePrimarySecretSuccessController.onPageLoad(FakeApplication.id).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture()

      running(fixture.playApplication) {
        val request =
          FakeRequest(POST, productionCredentialsChecklistRoute)
            .withFormUrlEncodedBody(("value[0]", ""))

        val boundForm = form.bind(Map("value[0]" -> ""))

        val view = fixture.playApplication.injector.instanceOf[ProductionCredentialsChecklistView]

        val result = route(fixture.playApplication, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, FakeApplication.id)(request, messages(fixture.playApplication)).toString
      }
    }

  }

}

object ProductionCredentialsChecklistControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  def buildFixture(userModel: UserModel = FakeUser, testConfiguration: Configuration = Configuration.empty): Fixture = {
    val apiHubService = mock[ApiHubService]
    when(apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id))(any()))
      .thenReturn(Future.successful(Some(FakeApplication)))

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel, testConfiguration)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
