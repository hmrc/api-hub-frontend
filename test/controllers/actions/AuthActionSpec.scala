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
import controllers.actions.AuthActionSpec.user
import models.user.{Permissions, StrideUser, UserModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class AuthActionSpec extends SpecBase with MockitoSugar {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth Action" - {

    "must redirect the user to sign in when unauthenticated in both Stride and LDAP" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.auth.routes.SignInController.onPageLoad().url
      }
    }

    "must allow the request to process when the user is authenticated with Stride" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(user)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "must redirect to the Unauthorised page when the user is unauthorised in Stride (ie wrong SRS role)" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthorised))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must allow the request to process when the user is authenticated with LDAP" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(user)))
      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "must show error page if stride user without email address" in {
      val strideAuth = mock[StrideAuthenticator]
      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(FakeUserStrideNoEmail)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())
        val content = contentAsString(result)
        content must include("You are not authorised to access this service")
        content must include("You do not have a valid email address associated with your Stride account.")
        status(result) mustBe OK
      }
    }

    "must show error page if ldap user without email address" in {
      val ldapAuth = mock[LdapAuthenticator]
      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(FakeUserLdapNoEmail)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())

        val content = contentAsString(result)
        content must include("You are not authorised to access this service")
        content must include("Your account does not have a linked email account.")
        status(result) mustBe OK
      }
    }
  }
}

object AuthActionSpec {

  val user: UserModel = UserModel(
    userId = "test-user-id",
    userName = "test-user-name",
    userType = StrideUser,
    email = Some("test-email"),
    permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
  )

}
