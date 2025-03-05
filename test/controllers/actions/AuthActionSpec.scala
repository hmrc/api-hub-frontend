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

import models.hubstatus.{FeatureStatus, FrontendShutter}
import models.user.{LdapUser, Permissions, StrideUser, UserModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.HubStatusService
import views.html.ShutteredView

import scala.concurrent.Future

class AuthActionSpec extends AnyFreeSpec with Matchers with MockitoSugar with OptionValues  {

  import AuthActionSpec.*

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth Action" - {

    "must redirect the user to sign in when unauthenticated in both Stride and LDAP" in {
      val fixture = buildFixture()

      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.auth.routes.SignInController.onPageLoad().url
      }
    }

    "must allow the request to process when the user is authenticated with Stride" in {
      val fixture = buildFixture()

      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(user)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "must redirect to the Unauthorised page when the user is unauthorised in Stride (ie wrong SRS role)" in {
      val fixture = buildFixture()

      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthorised))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must allow the request to process when the user is authenticated with LDAP" in {
      val fixture = buildFixture()

      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(user)))
      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "must show error page if stride user without email address" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserMissingEmail(user.userId, StrideUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())
        val content = contentAsString(result)
        content must include("You are not authorised to access this service")
        content must include("You do not have a valid email address associated with your STRIDE account.")
        status(result) mustBe OK
      }
    }

    "must show error page if ldap user without email address" in {
      val fixture = buildFixture()

      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserMissingEmail(user.userId, LdapUser)))
      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(notShuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        val content = contentAsString(result)
        content must include("You are not authorised to access this service")
        content must include("Your account does not have a linked email account.")
        status(result) mustBe OK
      }
    }

    "must show an unauthenticated user the shuttered page when the service is shuttered" in {
      val fixture = buildFixture()

      when(fixture.ldapAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsString(result) mustBe fixture.shutteredView(shutterMessage, None)(FakeRequest(), fixture.messages).toString
      }
    }

    "must show an authenticated non-support user the shuttered page when the service is shuttered" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(user)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsString(result) mustBe fixture.shutteredView(shutterMessage, Some(user))(FakeRequest(), fixture.messages).toString
      }
    }

    "must allow an authenticated support user to access the service when it is shuttered" in {
      val fixture = buildFixture()
      val supportUser = user.copy(permissions = user.permissions.copy(canSupport = true))

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(supportUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "must show an authenticated user with missing email the shuttered page when the service is shuttered" in {
      val fixture = buildFixture()

      when(fixture.strideAuthenticator.authenticate()(any())).thenReturn(Future.successful(UserMissingEmail(user.userId, StrideUser)))
      when(fixture.hubStatusService.status(any)).thenReturn(Future.successful(shuttered))

      running(fixture.application) {
        val authAction = fixture.application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

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

object AuthActionSpec {

  val user: UserModel = UserModel(
    userId = "test-user-id",
    userType = StrideUser,
    email = "test-email",
    permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
  )

  val shutterMessage = "test-shutter-message"

  val notShuttered: FeatureStatus = FeatureStatus(FrontendShutter, false, None)
  val shuttered: FeatureStatus = FeatureStatus(FrontendShutter, true, Some(shutterMessage))

  case class Fixture(
    ldapAuthenticator : LdapAuthenticator,
    strideAuthenticator: StrideAuthenticator,
    hubStatusService: HubStatusService,
    shutteredView: ShutteredView,
    messages: Messages,
    application: Application
  )

}
