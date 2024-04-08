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

import base.SpecBase
import controllers.actions.FakeUser
import forms.AddTeamMemberDetailsFormProvider
import models.UserAnswers
import models.application.TeamMember
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar
import pages.CreateTeamMembersPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.CreateTeamSessionRepository
import views.html.AddTeamMemberDetailsView

import scala.concurrent.Future

class CreateTeamMemberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AddTeamMemberDetailsFormProvider()
  val form = formProvider()

  lazy val createTeamMemberRoute = controllers.team.routes.CreateTeamMemberController.onPageLoad.url

  "CreateTeamMember Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, createTeamMemberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddTeamMemberDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, controllers.team.routes.CreateTeamMemberController.onSubmit, FakeUser)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[CreateTeamSessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val validEmails = Table("new.member@hmrc.gov.uk", "new.member@digital.hmrc.gov.uk", "new.member@HMRC.GOV.UK")
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[CreateTeamSessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        forAll(validEmails) { email =>
          val request =
            FakeRequest(POST, createTeamMemberRoute)
              .withFormUrlEncodedBody(("email", email))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val invalidEmails = Table("", "nope", "test@example.com", "user@other.hmrc.gov.uk")
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          forAll(invalidEmails) { email =>
            val request =
              FakeRequest(POST, createTeamMemberRoute).withFormUrlEncodedBody(("email", email))

            val boundForm = form.bind(Map("email" -> email))

            val view = application.injector.instanceOf[AddTeamMemberDetailsView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual
              view(boundForm, controllers.team.routes.CreateTeamMemberController.onSubmit, FakeUser)(request, messages(application)).toString
          }
        }
    }

    "must return an error when duplicate email is entered" in {
      val existingEmail = "existing@hmrc.gov.uk"
      val userAnswers = UserAnswers("id").set(CreateTeamMembersPage, Seq(TeamMember(existingEmail))).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, createTeamMemberRoute).withFormUrlEncodedBody(("email", existingEmail))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("This email address has already been added to this team")
      }
    }

    "must return an error when duplicate email ignoring case and leading/trailing space is entered" in {
      val existingEmail = "existing@hmrc.gov.uk"
      val userAnswers = UserAnswers("id").set(CreateTeamMembersPage, Seq(TeamMember(s"  ${existingEmail.toUpperCase}  "))).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, createTeamMemberRoute).withFormUrlEncodedBody(("email", existingEmail))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("This email address has already been added to this team")
      }
    }
  }
}
