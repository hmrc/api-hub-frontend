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
import forms.AddTeamMemberDetailsFormProvider
import models.application.TeamMember
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.TeamMembersPage
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import utils.HtmlValidation
import views.html.{AddTeamMemberDetailsView, ErrorTemplate}

import scala.concurrent.Future

class AddTeamMemberDetailsControllerSpec extends SpecBase with MockitoSugar with OptionValues with TryValues with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new AddTeamMemberDetailsFormProvider()
  private val form = formProvider()

  "AddTeamMemberDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, submitTo(NormalMode, 0), FakeUser)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must not populate the view on a GET when the question has previously been answered in Normal Mode" in {
      val userAnswers = UserAnswers(userAnswersId).set(TeamMembersPage, Seq(TeamMember("test.email@hmrc.gov.uk"))).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0).url)

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, submitTo(NormalMode, 0), FakeUser)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered in Check Mode" in {
      val email = "test.email@hmrc.gov.uk"
      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(TeamMember("creator.email@hmrc.gov.uk"), TeamMember(email)))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AddTeamMemberDetailsController.onPageLoad(CheckMode, 1).url)

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(TeamMember(email)), submitTo(CheckMode, 1), FakeUser)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(NormalMode, 0).url)
            .withFormUrlEncodedBody(("email", "test.email@hmrc.gov.uk"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(NormalMode, 0).url)
            .withFormUrlEncodedBody(("email", ""))

        val boundForm = form.bind(Map("email" -> ""))

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, submitTo(NormalMode, 0), FakeUser)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(NormalMode, 0).url)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must save the answer when it is not a duplicate in Normal Mode" in {
      val teamMember1 = TeamMember("existing.member@hmrc.gov.uk")
      val teamMember2 = TeamMember("new.member@hmrc.gov.uk")

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(teamMember1))
        .success
        .value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(NormalMode, 0).url)
            .withFormUrlEncodedBody(("email", teamMember2.email))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val updatedAnswers = userAnswers
          .set(TeamMembersPage, Seq(teamMember1, teamMember2))
          .success
          .value

        verify(mockSessionRepository).set(updatedAnswers)
      }
    }

    "must return a Bad Request and errors when a duplicate email is submitted in Normal Mode" in {
      val teamMember = TeamMember("existing.member@hmrc.gov.uk")

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(teamMember))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(NormalMode, 0).url)
            .withFormUrlEncodedBody(("email", teamMember.email))

        val boundForm = form
          .fill(teamMember)
          .withError(FormError("email", "addTeamMemberDetails.email.duplicate"))

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, submitTo(NormalMode, 0), FakeUser)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must save the answer when it is not a duplicate in Check Mode" in {
      val teamMember1 = TeamMember("existing.member1@hmrc.gov.uk")
      val teamMember2 = TeamMember("existing.member2@hmrc.gov.uk")
      val teamMember3 = TeamMember("existing.member3@hmrc.gov.uk")

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(teamMember1, teamMember2))
        .success
        .value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(CheckMode, 1).url)
            .withFormUrlEncodedBody(("email", teamMember3.email))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val updatedAnswers = userAnswers
          .set(TeamMembersPage, Seq(teamMember1, teamMember3))
          .success
          .value

        verify(mockSessionRepository).set(updatedAnswers)
      }
    }

    "must return a Bad Request and errors when a duplicate email is submitted in Check Mode" in {
      val teamMember1 = TeamMember("existing.member1@hmrc.gov.uk")
      val teamMember2 = TeamMember("existing.member2@hmrc.gov.uk")

      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(teamMember1, teamMember2))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(CheckMode, 1).url)
            .withFormUrlEncodedBody(("email", teamMember1.email))

        val boundForm = form
          .fill(teamMember1)
          .withError(FormError("email", "addTeamMemberDetails.email.duplicate"))

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, submitTo(CheckMode, 1), FakeUser)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return Not Found when the index is invalid for a GET in Check Mode" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(TeamMember("test-email")))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AddTeamMemberDetailsController.onPageLoad(CheckMode, 0).url)

        val view = application.injector.instanceOf[ErrorTemplate]

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "This page can’t be found",
            "Cannot find this team member."
          )(request, messages(application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return Not Found when the index is invalid for a POST in Check Mode" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(TeamMembersPage, Seq(TeamMember("test-email")))
        .success
        .value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddTeamMemberDetailsController.onSubmit(CheckMode, 1).url)
            .withFormUrlEncodedBody(("email", "test.email@hmrc.gov.uk"))

        val view = application.injector.instanceOf[ErrorTemplate]

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "This page can’t be found",
            "Cannot find this team member."
          )(request, messages(application))
            .toString()
        contentAsString(result) must validateAsHtml
      }

    }
  }

  private def submitTo(mode: Mode, index: Int): Call = {
    routes.AddTeamMemberDetailsController.onSubmit(mode, index)
  }

}
