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

package controllers.application

import base.SpecBase
import controllers.actions.{FakeApplication, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import forms.AddCredentialChecklistFormProvider
import models.api.ApiDetail
import models.application.{Api, Application, Credential, Primary}
import models.exception.ApplicationCredentialLimitException
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.AddCredentialSuccessViewModel
import views.html.application.{AddCredentialChecklistView, AddCredentialSuccessView}

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.concurrent.Future

class AddCredentialChecklistControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation {

  import AddCredentialChecklistControllerSpec._

  "AddCredentialChecklistController" - {
    "must return OK and the correct view for a GET for a team member or administrator" in {
      forAll(teamMemberAndAdministratorTable) {
        user =>
          val fixture = buildFixture(user)

          running(fixture.playApplication) {
            implicit val msgs: Messages = messages(fixture.playApplication)
            implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, addCredentialChecklistGetRoute())
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[AddCredentialChecklistView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, FakeApplication.id).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return the Add Credential Success view when valid data is submitted by a team member or administrator" in {
      forAll(teamMemberAndAdministratorTable) {
        user =>
          val api1 = ApiDetail("test-api-1", "test-api-name-1", "", "", Seq.empty, None, "")
          val api2 = ApiDetail("test-api-2", "test-api-name-2", "", "", Seq.empty, None, "")
          val apiNames = Seq(api1.title, api2.title)

          val application = FakeApplication.copy(
            apis = Seq(
              Api(api1.id, Seq.empty),
              Api(api2.id, Seq.empty)
            )
          )

          val fixture = buildFixture(user, Some(application))
          val credential = Credential("test-client-id", LocalDateTime.now(clock), Some("test-secret"), Some("test-fragment"))

          when(fixture.apiHubService.addCredential(ArgumentMatchers.eq(application.id), ArgumentMatchers.eq(Primary))(any()))
            .thenReturn(Future.successful(Right(Some(credential))))

          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(api1.id))(any()))
            .thenReturn(Future.successful(Some(api1)))

          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(api2.id))(any()))
            .thenReturn(Future.successful(Some(api2)))

          running(fixture.playApplication) {
            implicit val msgs: Messages = messages(fixture.playApplication)
            implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addCredentialChecklistPostRoute())
              .withFormUrlEncodedBody(("value[0]", "confirm"))

            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[AddCredentialSuccessView]

            val summaryList = AddCredentialSuccessViewModel.buildSummary(
              application,
              apiNames,
              credential
            )

            status(result) mustBe OK
            contentAsString(result) mustBe view(application, summaryList, Some(user), credential).toString()
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture()

      running(fixture.playApplication) {
        implicit val msgs: Messages = messages(fixture.playApplication)
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addCredentialChecklistPostRoute())
          .withFormUrlEncodedBody()

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[AddCredentialChecklistView]
        val formWithErrors = form.bind(Map.empty[String, String])

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, FakeApplication.id).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a team member or administrator" in {
      val fixture = buildFixture(FakeUserNotTeamMember)

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, addCredentialChecklistGetRoute())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must redirect to Unauthorised page for a POST when user is not a team member or administrator" in {
      val fixture = buildFixture(FakeUserNotTeamMember)

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addCredentialChecklistPostRoute())
          .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must return 400 Bad Request when the credential limit has been exceeded" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.addCredential(any(), any())(any()))
        .thenReturn(Future.successful(Left(ApplicationCredentialLimitException("test-message"))))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addCredentialChecklistPostRoute())
          .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must validateAsHtml
      }
    }
  }

}

object AddCredentialChecklistControllerSpec extends SpecBase with MockitoSugar {

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private def addCredentialChecklistGetRoute() = controllers.application.routes.AddCredentialChecklistController.onPageLoad(FakeApplication.id).url
  private def addCredentialChecklistPostRoute() = controllers.application.routes.AddCredentialChecklistController.onSubmit(FakeApplication.id).url
  private val formProvider = new AddCredentialChecklistFormProvider()
  private val form = formProvider()

  private def buildFixture(userModel: UserModel = FakeUser, application: Option[Application] = Some(FakeApplication)): Fixture = {
    val apiHubService = mock[ApiHubService]

    when(apiHubService.getApplication(any(), any())(any()))
      .thenReturn(Future.successful(application))

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
