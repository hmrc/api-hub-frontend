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

package controllers.admin

import base.SpecBase
import forms.admin.ApprovalDecisionFormProvider
import generators.{AccessRequestGenerator, ApplicationGenerator}
import models.accessrequest.Pending
import models.application.ApplicationLenses.*
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.admin.AccessRequestViewModel
import views.html.ErrorTemplate
import views.html.admin.AccessRequestView

import scala.concurrent.Future

class DeletedApplicationAccessRequestControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with TestHelpers
    with AccessRequestGenerator
    with ApplicationGenerator
    with HtmlValidation {

  import DeletedApplicationAccessRequestControllerSpec.*

  "DeletedApplicationAccessRequestController" - {
    "must return OK and the correct view for a GET for an admin user" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val application = sampleApplication()
        val accessRequest = sampleAccessRequest(application.id)

        when(fixture.apiHubService.getAccessRequest(any)(any)).thenReturn(Future.successful(Some(accessRequest)))
        when(fixture.apiHubService.getApplication(any, any, any)(any)).thenReturn(Future.successful(Some(application)))

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.DeletedApplicationAccessRequestController.onPageLoad(accessRequest.id))
          val result = route(fixture.application, request).value
          val model = AccessRequestViewModel.deletedApplicationViewModel(application, accessRequest, user)(messages(fixture.application))

          status(result) mustBe OK
          contentAsString(result) mustBe fixture.view(model, form, user, false)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService).getAccessRequest(eqTo(accessRequest.id))(any)
          verify(fixture.apiHubService).getApplication(eqTo(application.id), eqTo(false), eqTo(true))(any)
        }
      }
    }

    "must redirect to Unauthorised page for a GET when user is not an admin" in {
      forAll(usersWhoCannotViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.DeletedApplicationAccessRequestController.onPageLoad("test-access-request-di"))
          val result = route(fixture.application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must return 404 Not Found when the access request does not exist" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val accessRequestId = "test-access-request-id"

        when(fixture.apiHubService.getAccessRequest(any)(any)).thenReturn(Future.successful(None))

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.DeletedApplicationAccessRequestController.onPageLoad(accessRequestId))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ErrorTemplate]

          status(result) mustBe NOT_FOUND
          contentAsString(result) mustBe
              view(
                "Page not found - 404",
                "Production access request not found",
                s"Cannot find a production access request with ID $accessRequestId."
              )(request, messages(fixture.application))
                .toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val accessRequest = sampleAccessRequest()

        when(fixture.apiHubService.getAccessRequest(any)(any)).thenReturn(Future.successful(Some(accessRequest)))
        when(fixture.apiHubService.getApplication(any, any, any)(any)).thenReturn(Future.successful(None))

        running(fixture.application) {
          val request = FakeRequest(controllers.admin.routes.DeletedApplicationAccessRequestController.onPageLoad(accessRequest.id))
          val result = route(fixture.application, request).value
          val view = fixture.application.injector.instanceOf[ErrorTemplate]

          status(result) mustBe NOT_FOUND
          contentAsString(result) mustBe
            view(
              "Page not found - 404",
              "Application not found",
              s"Cannot find an application with ID ${accessRequest.applicationId}."
            )(request, messages(fixture.application))
              .toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }
  }

  private def buildFixture(user: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = user)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    val view = application.injector.instanceOf[AccessRequestView]

    Fixture(application, apiHubService, view)
  }

}

object DeletedApplicationAccessRequestControllerSpec {

  private val form = ApprovalDecisionFormProvider()()

  private case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService,
    view: AccessRequestView
  )

}
