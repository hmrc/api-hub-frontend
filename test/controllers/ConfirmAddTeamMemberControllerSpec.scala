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
import forms.ConfirmAddTeamMemberFormProvider
import models.{NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.ConfirmAddTeamMemberPage
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.checkAnswers.ConfirmAddTeamMember
import viewmodels.govuk.summarylist.SummaryListViewModel
import views.html.ConfirmAddTeamMemberView

class ConfirmAddTeamMemberControllerSpec extends SpecBase with MockitoSugar with OptionValues with TryValues {

  val formProvider = new ConfirmAddTeamMemberFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val confirmAddTeamMemberRoute: String = routes.ConfirmAddTeamMemberController.onPageLoad(NormalMode).url
  lazy val addTeamMemberRoute: String = routes.AddTeamMemberDetailsController.onPageLoad(NormalMode, 0).url
  lazy val checkYourAnswersRoute: String = routes.CheckYourAnswersController.onPageLoad.url

  "ConfirmAddTeamMember Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(ConfirmAddTeamMemberPage, true)
        .success
        .value
      val boundForm = form.bind(Map("value" -> "true"))
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      val teamMembers = SummaryListViewModel(
        rows = ConfirmAddTeamMember.rows(userAnswers)
      )
      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAddTeamMemberController.onPageLoad(NormalMode).url)

        val view = application.injector.instanceOf[ConfirmAddTeamMemberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        val actual = contentAsString(result)
        val expected = view(boundForm, teamMembers, Some(FakeUser), NormalMode)(request, messages(application)).toString
        actual mustEqual expected
      }
    }

    "must redirect to CheckYourAnswers page when selected 'no' for 'Do you need to add another team member?' form" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(ConfirmAddTeamMemberPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmAddTeamMemberController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val actualRedirectLocation = redirectLocation(result).value
        val expectedRedirectLocation = checkYourAnswersRoute

        actualRedirectLocation mustEqual expectedRedirectLocation
      }
    }

    "must redirect to AddTeamMemberDetails page when selected 'yes' for 'Do you need to add another team member?' form" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(ConfirmAddTeamMemberPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmAddTeamMemberController.onSubmit(NormalMode).url)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val actualRedirectLocation = redirectLocation(result).value
        val expectedRedirectLocation = addTeamMemberRoute

        actualRedirectLocation mustEqual expectedRedirectLocation
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ConfirmAddTeamMemberPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      val teamMembers = SummaryListViewModel(
        rows = ConfirmAddTeamMember.rows(userAnswers)
      )

      running(application) {
        val request =
          FakeRequest(POST, confirmAddTeamMemberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ConfirmAddTeamMemberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val actual = contentAsString(result)
        val expected = view(boundForm, teamMembers, Some(FakeUser), NormalMode)(request, messages(application)).toString
        actual mustEqual expected
      }
    }

  }
}
