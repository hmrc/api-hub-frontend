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

package controllers.admin

import base.SpecBase
import controllers.actions.{FakeApplication, FakeApprover, UserTypes}
import controllers.routes
import forms.admin.AccessRequestDecisionFormProvider
import generators.AccessRequestGenerator
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.admin.AccessRequestEndpointGroups
import views.html.ErrorTemplate
import views.html.admin.{AccessRequestApprovedSuccessView, AccessRequestView}

import scala.concurrent.Future

class AccessRequestControllerSpec
  extends SpecBase with MockitoSugar with HtmlValidation with TableDrivenPropertyChecks with UserTypes with AccessRequestGenerator {

  private val form = new AccessRequestDecisionFormProvider()()

  "AccessRequestController" - {
    "must return Ok and the correct view for an approver or support" in {
      forAll(usersWhoCanViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val accessRequest = sampleAccessRequest()

        when(fixture.apiHubService.getAccessRequest(any())(any())).thenReturn(Future.successful(Some(accessRequest)))
        when(fixture.apiHubService.getApplication(any(), any())(any())).thenReturn(Future.successful(Some(FakeApplication)))

        running(fixture.playApplication) {
          val url = controllers.admin.routes.AccessRequestController.onPageLoad(accessRequest.id).url
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
          implicit val msgs: Messages = messages(fixture.playApplication)
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[AccessRequestView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(accessRequest, FakeApplication, AccessRequestEndpointGroups.group(accessRequest), form, user).toString()
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService).getAccessRequest(ArgumentMatchers.eq(accessRequest.id))(any())
          verify(fixture.apiHubService).getApplication(ArgumentMatchers.eq(accessRequest.applicationId), ArgumentMatchers.eq(false))(any())
        }
      }
    }

    "must return 404 Not Found and a suitable explanation when the access request cannot be found" in {
      val fixture = buildFixture(FakeApprover)
      val id = "test-id"

      when(fixture.apiHubService.getAccessRequest(any())(any())).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val url = controllers.admin.routes.AccessRequestController.onPageLoad(id).url
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        implicit val msgs: Messages = messages(fixture.playApplication)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Production access request not found",
            s"Cannot find a production access request with Id $id."
          ).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return 404 Not Found and a suitable explanation when the application cannot be found" in {
      val fixture = buildFixture(FakeApprover)
      val accessRequest = sampleAccessRequest()

      when(fixture.apiHubService.getAccessRequest(any())(any())).thenReturn(Future.successful(Some(accessRequest)))
      when(fixture.apiHubService.getApplication(any(), any())(any())).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val url = controllers.admin.routes.AccessRequestController.onPageLoad(accessRequest.id).url
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        implicit val msgs: Messages = messages(fixture.playApplication)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with Id ${accessRequest.applicationId}."
          ).toString()
      }
    }

    "must redirect to the unauthorised page for a user who is not an approver or support" in {
      forAll(usersWhoCannotViewApprovals) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val url = controllers.admin.routes.AccessRequestController.onPageLoad("test-id").url
          val request = FakeRequest(GET, url)
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "on submission must approve an access request and return the success view" in {
      val fixture = buildFixture(FakeApprover)
      val id = "test-id"

      when(fixture.apiHubService.approveAccessRequest(any(), any())(any())).thenReturn(Future.successful(Some(())))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, controllers.admin.routes.AccessRequestController.onSubmit(id).url)
          .withFormUrlEncodedBody(("value", "approve"))
        implicit val msgs: Messages = messages(fixture.playApplication)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[AccessRequestApprovedSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeApprover).toString()
        contentAsString(result) must validateAsHtml
        verify(fixture.apiHubService).approveAccessRequest(ArgumentMatchers.eq(id), ArgumentMatchers.eq(FakeApprover.email.value))(any())
      }
    }

    "on submission must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(FakeApprover)
      val accessRequest = sampleAccessRequest()

      when(fixture.apiHubService.getAccessRequest(any())(any())).thenReturn(Future.successful(Some(accessRequest)))
      when(fixture.apiHubService.getApplication(any(), any())(any())).thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, controllers.admin.routes.AccessRequestController.onSubmit(accessRequest.id).url)
            .withFormUrlEncodedBody(("value", ""))
        implicit val msgs: Messages = messages(fixture.playApplication)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[AccessRequestView]
        val formWithErrors = form.bind(Map("value" -> ""))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe
          view(accessRequest, FakeApplication, AccessRequestEndpointGroups.group(accessRequest), formWithErrors, FakeApprover).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "on submission must redirect to the unauthorised page for a user who cannot approve" in {
      forAll(usersWhoCannotApprove) {(user: UserModel) =>
        val fixture = buildFixture(user)
        val id = "test-id"

        running(fixture.playApplication) {
          val request = FakeRequest(POST, controllers.admin.routes.AccessRequestController.onSubmit(id).url)
            .withFormUrlEncodedBody(("value", "approve"))
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
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
