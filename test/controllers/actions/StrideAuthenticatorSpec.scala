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

import controllers.actions.StrideAuthenticator.{API_HUB_APPROVER_ROLE, API_HUB_PRIVILEGED_USER_ROLE, API_HUB_SUPPORT_ROLE, API_HUB_USER_ROLE}
import controllers.actions.StrideAuthenticatorSpec.*
import models.user.{Permissions, StrideUser, UserModel}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}

import scala.concurrent.Future

class StrideAuthenticatorSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  "StrideAuthenticator" - {
    "must retrieve an authenticated user details correctly" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated approver details correctly" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = true, canSupport = false, isPrivileged = false)
      )

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated supporter details correctly" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = true, isPrivileged = false)
      )

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated privileged user's details correctly" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = true)
      )

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must return UserMissingEmail for a missing email address" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUserWithoutEmail(user)))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserMissingEmail(user.userId, StrideUser)
      }
    }

    "must return UserMissingEmail for an empty email address" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = " ",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserMissingEmail(user.userId, StrideUser)
      }
    }

    "must return unauthenticated when the user is not authenticated" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.failed(NoActiveSessionException))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserUnauthenticated
      }
    }

    "must return unauthorised when the user has insufficient enrolments" in {
      val authConnector = mock[AuthConnector]
      val strideAuthenticator = new StrideAuthenticator(authConnector)

      when(authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments("test-message")))

      strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserUnauthorised
      }
    }
  }

}

object StrideAuthenticatorSpec {

  type UserRetrieval = Retrieval[Enrolments ~ Option[String] ~ Option[Credentials]]

  val userRetrieval: UserRetrieval = Retrievals.authorisedEnrolments and Retrievals.email and Retrievals.credentials

  val userPredicate: Predicate =
    Enrolment(API_HUB_USER_ROLE) or
      Enrolment(API_HUB_APPROVER_ROLE) or
      Enrolment(API_HUB_SUPPORT_ROLE) or
      Enrolment(API_HUB_PRIVILEGED_USER_ROLE) and
      AuthProviders(PrivilegedApplication)

  val providerId: String = "test-provider-id"

  case object NoActiveSessionException extends NoActiveSession("test-message")

  def retrievalsForUser(user: UserModel): Enrolments ~ Option[String] ~ Option[Credentials] = {
      uk.gov.hmrc.auth.core.retrieve.~(
        uk.gov.hmrc.auth.core.retrieve.~(
          enrolmentsFor(user),
          Some(user.email)
        ),
        credentialsForUser()
      )
  }

  def retrievalsForUserWithoutEmail(user: UserModel): Enrolments ~ Option[String] ~ Option[Credentials] = {
    uk.gov.hmrc.auth.core.retrieve.~(
      uk.gov.hmrc.auth.core.retrieve.~(
        enrolmentsFor(user),
        None
      ),
      credentialsForUser()
    )
  }

  private def enrolmentsFor(user: UserModel): Enrolments = {
    if (user.permissions.canApprove) {
      Enrolments(
        Set(Enrolment(key = API_HUB_APPROVER_ROLE))
      )
    }
    else if (user.permissions.canSupport) {
      Enrolments(
        Set(Enrolment(key = API_HUB_SUPPORT_ROLE))
      )
    }
    else if (user.permissions.isPrivileged) {
      Enrolments(
        Set(Enrolment(key = API_HUB_PRIVILEGED_USER_ROLE))
      )
    }
    else {
      Enrolments(
        Set(Enrolment(key = API_HUB_USER_ROLE))
      )
    }
  }

  private def credentialsForUser(): Option[Credentials] = {
    Some(Credentials(providerId, PrivilegedApplication.toString))
  }

}
