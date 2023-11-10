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
import models.user.{Permissions, StrideUser, UserModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class OptionalIdentifierActionSpec extends SpecBase with MockitoSugar {

  import OptionalIdentifierActionSpec._

  "OptionalIdentifierAction" - {
    "must identify a user when authenticated in Stride" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(testUser)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe OK
      }
    }

    "must identify a user when authenticated in LDAP" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserAuthenticated(testUser)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe OK
      }
    }

    "must not identify a user when they have no auth token" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(unauthenticatedRequest())

        status(result) mustBe NO_CONTENT
        verifyNoMoreInteractions(ldapAuth, strideAuth)
      }
    }

    "must not identify a user when unauthenticated in both Stride and LDAP" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))
      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe NO_CONTENT
      }
    }

    "must not identify a user when unauthorised in Stride" in {
      val ldapAuth = mock[LdapAuthenticator]
      val strideAuth = mock[StrideAuthenticator]

      when(strideAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthorised))
      when(ldapAuth.authenticate()(any())).thenReturn(Future.successful(UserUnauthenticated))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[LdapAuthenticator].toInstance(ldapAuth),
          bind[StrideAuthenticator].toInstance(strideAuth)
        )
        .build()

      running(application) {
        val authAction = application.injector.instanceOf[OptionallyAuthenticatedIdentifierAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(authenticatedRequest())

        status(result) mustBe NO_CONTENT
      }
    }
  }

}

object OptionalIdentifierActionSpec {

  class Harness(authAction: OptionalIdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction {
      implicit request =>
        request.user match {
          case Some(user) if user == testUser => Results.Ok
          case Some(_) => Results.InternalServerError
          case None => Results.NoContent
        }
    }
  }

  val testUser: UserModel = UserModel(
    userId = "test-user-id",
    userName = "test-user-name",
    userType = StrideUser,
    email = Some("test-email"),
    permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
  )

  def unauthenticatedRequest(): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest()
  }

  def authenticatedRequest(): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest().withSession((SessionKeys.authToken, "test-auth-token"))
  }

}
