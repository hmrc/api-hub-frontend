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

import controllers.actions.LdapAuthenticatorSpec._
import models.user.{LdapUser, Permissions, UserModel}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._

import scala.concurrent.Future

class LdapAuthenticatorSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  "LdapAuthenticator" - {
    "must retrieve an authenticated user details correctly" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      val user = UserModel(
        userId = s"LDAP-$username",
        userType = LdapUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(mockStubBehaviour.stubAuth(eqTo(None), eqTo(retrieval)))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      ldapAuthenticator.authenticate()(requestWithAuthorisation).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated approver details correctly" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      val user = UserModel(
        userId = s"LDAP-$username",
        userType = LdapUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = true, canSupport = false, isPrivileged = false)
      )

      when(mockStubBehaviour.stubAuth(eqTo(None), eqTo(retrieval)))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      ldapAuthenticator.authenticate()(requestWithAuthorisation).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated supporter details correctly" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      val user = UserModel(
        userId = s"LDAP-$username",
        userType = LdapUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = true, isPrivileged = false)
      )

      when(mockStubBehaviour.stubAuth(eqTo(None), eqTo(retrieval)))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      ldapAuthenticator.authenticate()(requestWithAuthorisation).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated privileged user's details correctly" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      val user = UserModel(
        userId = s"LDAP-$username",
        userType = LdapUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = true)
      )

      when(mockStubBehaviour.stubAuth(eqTo(None), eqTo(retrieval)))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      ldapAuthenticator.authenticate()(requestWithAuthorisation).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must return unauthenticated when the user is not authenticated" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      when(mockStubBehaviour.stubAuth(eqTo(None), eqTo(retrieval)))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorised", Status.UNAUTHORIZED)))

      ldapAuthenticator.authenticate()(requestWithAuthorisation).map {
        result =>
          result mustBe UserUnauthenticated
      }
    }

    "must return unauthenticated when the request has no authorisation" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      ldapAuthenticator.authenticate()(requestWithoutAuthorisation).map {
        result =>
          result mustBe UserUnauthenticated
      }
    }

    "must return UserMissingEmail for a missing email address" in {
      implicit val cc: ControllerComponents = stubMessagesControllerComponents()
      val mockStubBehaviour = mock[StubBehaviour]
      val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)
      val ldapAuthenticator = new LdapAuthenticator(stubAuth)

      val user = UserModel(
        userId = s"LDAP-$username",
        userType = LdapUser,
        email = "",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(mockStubBehaviour.stubAuth(eqTo(None), eqTo(retrieval)))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      ldapAuthenticator.authenticate()(requestWithAuthorisation).map {
        result =>
          result mustBe UserMissingEmail(user.userId, LdapUser)
      }
    }
  }

}

object LdapAuthenticatorSpec {

  val requestWithAuthorisation: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.authToken -> "Open sesame")
  val requestWithoutAuthorisation: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val username = "test-user-name"

  private val canApprovePredicate = Predicate.Permission(
    Resource(
      ResourceType("api-hub-frontend"),
      ResourceLocation("approvals")
    ),
    IAAction("WRITE")
  )

  private val canSupportPredicate = Predicate.Permission(
    Resource(
      ResourceType("api-hub-frontend"),
      ResourceLocation("support")
    ),
    IAAction("WRITE")
  )

  private val isPrivilegedPredicate = Predicate.Permission(
    Resource(
      ResourceType("api-hub-frontend"),
      ResourceLocation("privileged-usage")
    ),
    IAAction("WRITE")
  )

  val retrieval: Retrieval.NonEmptyRetrieval[Retrieval.Username ~ Option[Retrieval.Email] ~ Boolean ~ Boolean ~ Boolean]
    = Retrieval.username ~
      Retrieval.email ~
      Retrieval.hasPredicate(canApprovePredicate) ~
      Retrieval.hasPredicate(canSupportPredicate) ~
    Retrieval.hasPredicate(isPrivilegedPredicate)

  def retrievalsForUser(user: UserModel): Retrieval.Username ~ Option[Retrieval.Email] ~ Boolean ~ Boolean ~ Boolean = {
    val email = if (user.email.nonEmpty) {
      Some(Retrieval.Email(user.email))
    }
    else {
      None
    }

    uk.gov.hmrc.internalauth.client.~(
      uk.gov.hmrc.internalauth.client.~(
        uk.gov.hmrc.internalauth.client.~(
          uk.gov.hmrc.internalauth.client.~(
            Retrieval.Username(username),
            email
          ),
          user.permissions.canApprove
        ),
        user.permissions.canSupport
      ),
      user.permissions.isPrivileged
    )
  }

}
