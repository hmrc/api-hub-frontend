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

package controllers.application

import base.SpecBase
import controllers.actions.{FakeApiDetail, FakeApplication, FakeApprover, FakeUser}
import forms.YesNoFormProvider
import models.api.ApiDetail
import models.application.{Api, Application}
import models.application.ApplicationLenses._
import models.user.UserModel
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.application.{RemoveApiConfirmationView, RemoveApiSuccessView}

import scala.concurrent.Future

class RemoveApiControllerSpec extends SpecBase with MockitoSugar with ArgumentMatchersSugar with HtmlValidation with TestHelpers {

  import RemoveApiControllerSpec._

  "onPageLoad" - {
    "must display the correct confirmation view for a given application and API for a team member or supporter" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val fixture = buildFixture(user)

          when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
            .thenReturn(Future.successful(Some(application)))

          when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
            .thenReturn(Future.successful(Some(apiDetail)))

          running(fixture.playApplication) {
            val request = FakeRequest(controllers.application.routes.RemoveApiController.onPageLoad(application.id, apiDetail.id))
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[RemoveApiConfirmationView]

            status(result) mustBe OK
            contentAsString(result) mustBe view(application, apiDetail, form, user)(request, messages(fixture.playApplication)).toString()
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must redirect to the Unauthorised page when the user is not support or a team member" in {
      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onPageLoad(application.id, apiDetail.id))
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must return a 404 Not Found with suitable message when the API has not been added to the application" in {
      val fixture = buildFixture(FakeUser)

      when(fixture.apiHubService.getApplication(eqTo(applicationWithoutApis.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(applicationWithoutApis)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onPageLoad(applicationWithoutApis.id, apiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"The API ${apiDetail.title} has not been added to the application ${applicationWithoutApis.name}."
          )(request, messages(fixture.playApplication)).toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must return a 404 Not Found with suitable message when the API does not exist" in {
      val fixture = buildFixture(FakeUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onPageLoad(application.id, apiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id ${apiDetail.id}."
          )(request, messages(fixture.playApplication)).toString()

        contentAsString(result) must validateAsHtml
      }
    }
  }

  "onSubmit" - {
    "must process the request and show the success page for a team member or support" in {
      forAll(teamMemberAndSupporterTable) {
        user: UserModel =>
          val fixture = buildFixture(user)

          when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
            .thenReturn(Future.successful(Some(application)))

          when(fixture.apiHubService.removeApi(eqTo(application.id), eqTo(apiDetail.id))(any))
            .thenReturn(Future.successful(Some(())))

          running(fixture.playApplication) {
            val request = FakeRequest(controllers.application.routes.RemoveApiController.onSubmit(application.id, apiDetail.id))
              .withFormUrlEncodedBody(("value", "true"))

            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[RemoveApiSuccessView]

            status(result) mustBe OK
            contentAsString(result) mustBe view(application, user)(request, messages(fixture.playApplication)).toString()
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return Bad Request and errors when the submitted data is invalid" in {
      val fixture = buildFixture(FakeUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onSubmit(application.id, apiDetail.id))
          .withFormUrlEncodedBody(("value", ""))

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[RemoveApiConfirmationView]

        val formWithErrors = form.bind(Map("value" -> ""))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(application, apiDetail, formWithErrors, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the Unauthorised page when the user is not support or a team member" in {
      val fixture = buildFixture(FakeApprover)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onSubmit(application.id, apiDetail.id))
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must return Not Found and a suitable message when the service responds that the API/application are not found" in {
      val fixture = buildFixture(FakeUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(application)))

      when(fixture.apiHubService.removeApi(eqTo(application.id), eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onSubmit(application.id, apiDetail.id))
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"The API ${apiDetail.id} has not been added to the application ${applicationWithoutApis.name}."
          )(request, messages(fixture.playApplication)).toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the application details page when the user answers no" in {
      val fixture = buildFixture(FakeUser)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any))
        .thenReturn(Future.successful(Some(application)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.routes.RemoveApiController.onSubmit(application.id, apiDetail.id))
          .withFormUrlEncodedBody(("value", "false"))

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.application.routes.ApplicationDetailsController.onPageLoad(application.id).url
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}

object RemoveApiControllerSpec {

  val form: Form[_] = new YesNoFormProvider()("removeApiConfirmation.error")
  val apiDetail: ApiDetail = FakeApiDetail
  val application: Application = FakeApplication.addApi(Api(apiDetail.id, Seq.empty))
  val applicationWithoutApis: Application = FakeApplication

}
