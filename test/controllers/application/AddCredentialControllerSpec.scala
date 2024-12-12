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
import controllers.actions.{FakeApplication, FakePrivilegedUser, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import forms.AddCredentialChecklistFormProvider
import models.api.{ApiDetail, Live, Maintainer}
import models.application.{Api, Application, Credential, Primary, Secondary}
import models.exception.ApplicationCredentialLimitException
import models.user.{Permissions, UserModel}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.AddCredentialSuccessViewModel
import views.html.application.{AddCredentialChecklistView, AddCredentialSuccessView}

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.concurrent.Future

class AddCredentialControllerSpec extends SpecBase with MockitoSugar with TestHelpers with HtmlValidation with TableDrivenPropertyChecks {

  import AddCredentialControllerSpec._

  "AddCredentialChecklistController.checklist" - {
    "must return OK and the correct view for a GET for a privileged user who is a team member or support" in {
      forAll(privilegedTeamMemberAndSupporterTable) {
        user =>
          val fixture = buildFixture(user)

          running(fixture.playApplication) {
            implicit val msgs: Messages = messages(fixture.playApplication)
            implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, checklistRoute())
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[AddCredentialChecklistView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, FakeApplication.id).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must redirect to Unauthorised page for a GET for a team member or supporter who is not a privileged user" in {
      forAll(teamMemberAndSupporterTable) {
        user =>
          val fixture = buildFixture(user)

          running(fixture.playApplication) {
            implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, checklistRoute())
            val result = route(fixture.playApplication, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
          }
      }
    }

    "must redirect to Unauthorised page for a GET for a privileged user who is not a team member" in {
      val fixture = buildFixture(FakePrivilegedUser)

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, checklistRoute())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }
  }

  "AddCredentialChecklistController.addProductionCredential" - {
    "must return the Add Credential Success view when valid data is submitted by a a privileged user who is a team member or supporter" in {
      forAll(privilegedTeamMemberAndSupporterTable) {
        user =>
          val api1 = ApiDetail("test-api-1", "pubRef1", "test-api-name-1", "", "", Seq.empty, None, "", Live,
            reviewedDate = Instant.now(), platform = "HIP", maintainer = Maintainer("name", "#slack", List.empty))
          val api2 = ApiDetail("test-api-2", "pubRef2", "test-api-name-2", "", "", Seq.empty, None, "", Live,
            reviewedDate = Instant.now(), platform = "HIP", maintainer = Maintainer("name", "#slack", List.empty))
          val apiNames = Seq(api1.title, api2.title)

          val application = FakeApplication.copy(
            apis = Seq(
              Api(api1.id, api1.title, Seq.empty),
              Api(api2.id, api2.title, Seq.empty)
            )
          )

          val fixture = buildFixture(user, Some(application))
          val credential = Credential("test-client-id", LocalDateTime.now(clock), Some("test-secret"), Some("test-fragment"))

          when(fixture.apiHubService.addCredential(eqTo(application.id), argThat(hipEnvironment => hipEnvironment.isProductionLike))(any()))
            .thenReturn(Future.successful(Right(Some(credential))))

          when(fixture.apiHubService.getApiDetail(eqTo(api1.id))(any()))
            .thenReturn(Future.successful(Some(api1)))

          when(fixture.apiHubService.getApiDetail(eqTo(api2.id))(any()))
            .thenReturn(Future.successful(Some(api2)))

          running(fixture.playApplication) {
            implicit val msgs: Messages = messages(fixture.playApplication)
            implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addProductionCredentialRoute())
              .withFormUrlEncodedBody(("value[0]", "confirm"))

            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[AddCredentialSuccessView]
            val credentialsPageUrl = controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "production").url

            val summaryList = AddCredentialSuccessViewModel.buildSummary(
              application,
              apiNames,
              credential
            )

            status(result) mustBe OK
            contentAsString(result) mustBe view(application, summaryList, Some(user), credential, credentialsPageUrl).toString()
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture(FakeUser.copy(permissions = FakeUser.permissions.copy(isPrivileged = true)))

      running(fixture.playApplication) {
        implicit val msgs: Messages = messages(fixture.playApplication)
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addProductionCredentialRoute())
          .withFormUrlEncodedBody()

        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[AddCredentialChecklistView]
        val formWithErrors = form.bind(Map.empty[String, String])

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, FakeApplication.id).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a POST when the user is a privileged user but not a team member or support" in {
      val fixture = buildFixture(FakePrivilegedUser)

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addProductionCredentialRoute())
          .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must return 400 Bad Request when the credential limit has been exceeded" in {
      val fixture = buildFixture(FakeUser.copy(permissions = FakeUser.permissions.copy(isPrivileged = true)))

      when(fixture.apiHubService.addCredential(any(), any())(any()))
        .thenReturn(Future.successful(Left(ApplicationCredentialLimitException("test-message"))))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, addProductionCredentialRoute())
          .withFormUrlEncodedBody(("value[0]", "confirm"))

        val result = route(fixture.playApplication, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must validateAsHtml
      }
    }
  }

  "AddCredentialChecklistController.addDevelopmentCredential" - {
    "must add the credential and redirect to the Environments and Credentials page" in {
      val fixture = buildFixture()

      val credential = Credential("test-client-id", LocalDateTime.now(clock), Some("test-secret"), Some("test-fragment"))

      when(fixture.apiHubService.addCredential(eqTo(FakeApplication.id), argThat(hipEnvironment => !hipEnvironment.isProductionLike))(any()))
        .thenReturn(Future.successful(Right(Some(credential))))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, addDevelopmentCredentialRoute())

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "test").url
      }
    }

    "must redirect to Unauthorised page for a POST when user is not a team member or supporter" in {
      val fixture = buildFixture(FakeUserNotTeamMember)

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, addDevelopmentCredentialRoute())

        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must return 400 Bad Request when the credential limit has been exceeded" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.addCredential(any(), any())(any()))
        .thenReturn(Future.successful(Left(ApplicationCredentialLimitException("test-message"))))

      running(fixture.playApplication) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, addDevelopmentCredentialRoute())

        val result = route(fixture.playApplication, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must validateAsHtml
      }
    }
  }

  "AddCredentialChecklistController.addCredentialForEnvironment" - {
    "must give the correct response for given user and environment" in {
      val FakeSupporterOnApplicationTeam = FakeUser.copy(permissions = Permissions(false, true, false))
      val FakePrivilegedUserOnApplicationTeam = FakeUser.copy(permissions = Permissions(false, false, true))
      val scenarios = Table(
        ("user", "environment", "expected response", "redirect location"),
        (FakeUser, "test", SEE_OTHER, Some(() => controllers.application.routes.EnvironmentsController.onPageLoad(FakeApplication.id, "test").url)),
        (FakeUserNotTeamMember, "test", SEE_OTHER, Some(() => routes.UnauthorisedController.onPageLoad.url)),

        (FakeUser, "production", SEE_OTHER, Some(() => routes.UnauthorisedController.onPageLoad.url)),
        (FakeSupporterOnApplicationTeam, "production", SEE_OTHER, Some(() => routes.UnauthorisedController.onPageLoad.url)),
        (FakePrivilegedUser, "production", SEE_OTHER, Some(() => routes.UnauthorisedController.onPageLoad.url)),
        (FakePrivilegedUserOnApplicationTeam, "production", OK, None)
      )

      forAll(scenarios) { (user: UserModel, environment: String, expectedResponse: Int, redirectTo: Option[() => String]) =>
        val fixture = buildFixture(user)
        val credential = Credential("test-client-id", LocalDateTime.now(clock), Some("test-secret"), Some("test-fragment"))
        when(fixture.apiHubService.addCredential(eqTo(FakeApplication.id), any())(any()))
          .thenReturn(Future.successful(Right(Some(credential))))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.application.routes.AddCredentialController.addCredentialForEnvironment(FakeApplication.id, environment))
          val result = route(fixture.playApplication, request).value
          status(result) mustBe expectedResponse
          redirectTo match {
            case Some(urlBuilder) => redirectLocation(result).value mustBe urlBuilder()
            case None => ()
          }
        }
      }
    }
  }
}

object AddCredentialControllerSpec extends SpecBase with MockitoSugar {

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private def checklistRoute() = controllers.application.routes.AddCredentialController.checklist(FakeApplication.id).url
  private def addProductionCredentialRoute() = controllers.application.routes.AddCredentialController.addProductionCredential(FakeApplication.id).url
  private def addDevelopmentCredentialRoute() = controllers.application.routes.AddCredentialController.addDevelopmentCredential(FakeApplication.id).url
  private val formProvider = new AddCredentialChecklistFormProvider()
  private val form = formProvider()

  private def buildFixture(userModel: UserModel = FakeUser, application: Option[Application] = Some(FakeApplication)): Fixture = {
    val apiHubService = mock[ApiHubService]

    when(apiHubService.getApplication(any(), any(), any())(any()))
      .thenReturn(Future.successful(application))

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
