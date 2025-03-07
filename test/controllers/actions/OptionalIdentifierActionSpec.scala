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

package controllers.actions

import base.SpecBase
import models.hubstatus.{FeatureStatus, FrontendShutter}
import models.user.{Permissions, StrideUser, UserModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verifyNoMoreInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.HubStatusService
import uk.gov.hmrc.http.SessionKeys
import views.html.ShutteredView

import scala.concurrent.Future

class OptionalIdentifierActionSpec extends SpecBase with MockitoSugar {

  import OptionalIdentifierActionSpec._

  "OptionalIdentifierAction" - {
    "must identify a user when authenticated in Stride" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(testUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe OK
      }
    }

    "must identify a user when authenticated in LDAP" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(testUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe OK
      }
    }

    "must not identify a user when they have no auth token" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(unauthenticatedRequest())

        status(result) mustBe NO_CONTENT
        verifyNoMoreInteractions(fixture.ldapAuthenticator, fixture.strideAuthenticator)
      }
    }

    "must not identify a user when unauthenticated in both Stride and LDAP" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe NO_CONTENT
      }
    }

    "must not identify a user when unauthorised in Stride" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthorised))
      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe NO_CONTENT
      }
    }

    "must not identify a user with missing email in Stride" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserMissingEmail(testUser.userId, testUser.userType)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe NO_CONTENT
      }
    }

    "must not identify a user with missing email in LDAP" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserMissingEmail(testUser.userId, testUser.userType)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe NO_CONTENT
      }
    }

    "must show an unauthenticated user the shuttered page when the service is shuttered" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsString(result) mustBe fixture.shutteredView(shutterMessage, None)(FakeRequest(), fixture.messages).toString
      }
    }

    "must show an authenticated non-support user the shuttered page when the service is shuttered" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(testUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsString(result) mustBe fixture.shutteredView(shutterMessage, Some(testUser))(FakeRequest(), fixture.messages).toString
      }
    }

    "must allow an authenticated support user to access the service when it is shuttered" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(supportUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe OK
      }
    }

    "must show an authenticated user with missing email the shuttered page when the service is shuttered" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserMissingEmail(testUser.userId, testUser.userType)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsString(result) mustBe fixture.shutteredView(shutterMessage, None)(FakeRequest(), fixture.messages).toString
      }
    }
  }

  def buildFixture(): Fixture = {
    val ldapAuthenticator = mock[LdapAuthenticator]
    val strideAuthenticator = mock[StrideAuthenticator]
    val hubStatusService = mock[HubStatusService]

    val application = GuiceApplicationBuilder()
      .overrides(
        bind[LdapAuthenticator].toInstance(ldapAuthenticator),
        bind[StrideAuthenticator].toInstance(strideAuthenticator),
        bind[HubStatusService].toInstance(hubStatusService)
      )
      .build()

    val shutteredView = application.injector.instanceOf[ShutteredView]
    val messages = application.injector.instanceOf[MessagesApi].preferred(FakeRequest())

    Fixture(ldapAuthenticator, strideAuthenticator, hubStatusService, shutteredView, messages, application)
  }

}

object OptionalIdentifierActionSpec {

  class Harness(authAction: OptionalIdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction {
      implicit request =>
        request.user match {
          case Some(user) if user == testUser => Results.Ok
          case Some(user) if user == supportUser => Results.Ok
          case Some(_) => Results.InternalServerError
          case None => Results.NoContent
        }
    }
  }

  val testUser: UserModel = UserModel(
    userId = "test-user-id",
    userType = StrideUser,
    email = "test-email",
    permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
  )

  val supportUser: UserModel = testUser.copy(permissions = testUser.permissions.copy(canSupport = true))

  val shutterMessage = "test-shutter-message"

  val notShuttered: FeatureStatus = FeatureStatus(FrontendShutter, false, None)
  val shuttered: FeatureStatus = FeatureStatus(FrontendShutter, true, Some(shutterMessage))

  def unauthenticatedRequest(): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest()
  }

  def authenticatedRequest(): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest().withSession((SessionKeys.authToken, "test-auth-token"))
  }

  case class Fixture(
    ldapAuthenticator : LdapAuthenticator,
    strideAuthenticator: StrideAuthenticator,
    hubStatusService: HubStatusService,
    shutteredView: ShutteredView,
    messages: Messages,
    application: Application
  )

}
