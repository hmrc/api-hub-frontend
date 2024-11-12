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

import controllers.actions.StrideAuthenticator.*
import controllers.actions.StrideAuthenticatorSpec.*
import models.user.{Permissions, StrideUser, UserModel}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.MetricsService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class StrideAuthenticatorSpec extends AsyncFreeSpec
  with Matchers
  with MockitoSugar
  with TableDrivenPropertyChecks {

  "StrideAuthenticator" - {
    "must retrieve an authenticated user details correctly" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated approver details correctly" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = true, canSupport = false, isPrivileged = false)
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated supporter details correctly" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = true, isPrivileged = false)
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must retrieve an authenticated privileged user's details correctly" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = true)
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserAuthenticated(user)
      }
    }

    "must return UserMissingEmail for a missing email address" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUserWithoutEmail(user)))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserMissingEmail(user.userId, StrideUser)
      }
    }

    "must return UserMissingEmail for an empty email address" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = " ",
        permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUser(user)))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          verify(fixture.metricsService).strideMissingEmail()
          result mustBe UserMissingEmail(user.userId, StrideUser)
      }
    }

    "must return unauthenticated when the user is not authenticated" in {
      val fixture = buildFixture()

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.failed(NoActiveSessionException))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserUnauthenticated
      }
    }

    "must return unauthorised when the user has insufficient enrolments" in {
      val fixture = buildFixture()

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments("test-message")))

      fixture.strideAuthenticator.authenticate()(FakeRequest()).map {
        result =>
          result mustBe UserUnauthorised
      }
    }

    "must throw UnauthorizedException when the providerId retrieval is missing" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
      )

      when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
        .thenReturn(Future.successful(retrievalsForUserWithoutCredentials(user)))

      recoverToSucceededIf[UnauthorizedException] {
        fixture.strideAuthenticator.authenticate()(FakeRequest())
      }
    }

    "must work with both API Hub and IPAAS roles" in {
      val fixture = buildFixture()

      val user = UserModel(
        userId = s"STRIDE-$providerId",
        userType = StrideUser,
        email = "jo.bloggs@email.com",
      )

      forAll(
        Table(
          ("hip roles", "ipaasRole", "permissions"),
          (Seq(API_HUB_USER_ROLE), IPAAS_LIVE_SERVICE, Permissions(false, false, false)),
          (Seq(API_HUB_SUPPORT_ROLE, API_HUB_APPROVER_ROLE), IPAAS_LIVE_ADMINS, Permissions(true, true, false)),
          (Seq(API_HUB_PRIVILEGED_USER_ROLE), IPAAS_LIVE_SERVICE_SC, Permissions(false, false, true)),
        )
      ) { case (hubRoles: Seq[String] @unchecked, ipassRole: String, permissions: Permissions) =>

        val userWithPermissions = user.copy(
          permissions = permissions
        )
        val hubEnrolments = hubRoles.map(Enrolment(_)).toSet
        val ipaasEnrolments = hubRoles.map(Enrolment(_)).toSet

        when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
          .thenReturn(Future.successful(retrievalsForUser(userWithPermissions, hubEnrolments)))

        val userWithHubRoles = fixture.strideAuthenticator.authenticate()(FakeRequest())

        when(fixture.authConnector.authorise(eqTo(userPredicate), eqTo(userRetrieval))(any(), any()))
          .thenReturn(Future.successful(retrievalsForUser(userWithPermissions, ipaasEnrolments)))

        val userWithIpassRoles = fixture.strideAuthenticator.authenticate()(FakeRequest())

        Future.sequence(Seq(userWithHubRoles, userWithIpassRoles)).map(
         _.distinct must have size 1
        )
      }
    }
  }

  private def buildFixture(): Fixture = {
    val authConnector = mock[AuthConnector]
    val metricsService = mock[MetricsService]
    val strideAuthenticator = new StrideAuthenticator(authConnector, metricsService)

    Fixture(authConnector, metricsService, strideAuthenticator)
  }

}

object StrideAuthenticatorSpec {

  type UserRetrieval = Retrieval[Enrolments ~ Option[String] ~ Option[Credentials]]

  val userRetrieval: UserRetrieval = Retrievals.authorisedEnrolments and Retrievals.email and Retrievals.credentials

  def userPredicate: Predicate =
    (approverRoles ++ privilegedRoles ++ supportRoles ++ userRoles)
      .foldRight[Predicate](EmptyPredicate) {
        case (role, EmptyPredicate) =>
          Enrolment(role)
        case (role, acc) =>
          Enrolment(role) or acc
      } and AuthProviders(PrivilegedApplication)

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

  def retrievalsForUser(user: UserModel, enrolments: Set[Enrolment]): Enrolments ~ Option[String] ~ Option[Credentials] = {
    uk.gov.hmrc.auth.core.retrieve.~(
      uk.gov.hmrc.auth.core.retrieve.~(
        Enrolments(enrolments),
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

  def retrievalsForUserWithoutCredentials(user: UserModel): Enrolments ~ Option[String] ~ Option[Credentials] = {
    uk.gov.hmrc.auth.core.retrieve.~(
      uk.gov.hmrc.auth.core.retrieve.~(
        enrolmentsFor(user),
        Some(user.email)
      ),
      None
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

  private case class Fixture(
    authConnector: AuthConnector,
    metricsService: MetricsService,
    strideAuthenticator: StrideAuthenticator
  )

}
