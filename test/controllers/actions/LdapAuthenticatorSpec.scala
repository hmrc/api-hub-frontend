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

import controllers.actions.LdapAuthenticatorSpec.{approverRetrieval, requestWithAuthorisation, requestWithoutAuthorisation, retrievalsForUser}
import models.user.{LdapUser, Permissions, UserModel}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
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
        userId = "LDAP-jo.bloggs",
        userName = "jo.bloggs",
        userType = LdapUser,
        email = Some("jo.bloggs@email.com"),
        permissions = Permissions(canApprove = false, canAdminister = false)
      )

      when(mockStubBehaviour.stubAuth(ArgumentMatchers.eq(None), ArgumentMatchers.eq(approverRetrieval)))
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
        userId = "LDAP-jo.bloggs",
        userName = "jo.bloggs",
        userType = LdapUser,
        email = Some("jo.bloggs@email.com"),
        permissions = Permissions(canApprove = true, canAdminister = false)
      )

      when(mockStubBehaviour.stubAuth(ArgumentMatchers.eq(None), ArgumentMatchers.eq(approverRetrieval)))
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

      when(mockStubBehaviour.stubAuth(ArgumentMatchers.eq(None), ArgumentMatchers.eq(approverRetrieval)))
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
  }

}

object LdapAuthenticatorSpec {

  val requestWithAuthorisation: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.authToken -> "Open sesame")
  val requestWithoutAuthorisation: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val canApprovePredicate = Predicate.Permission(
    Resource(
      ResourceType("api-hub-frontend"),
      ResourceLocation("approvals")
    ),
    IAAction("WRITE")
  )

  val approverRetrieval: Retrieval.NonEmptyRetrieval[Retrieval.Username ~ Option[Retrieval.Email] ~ Boolean]
    = Retrieval.username ~ Retrieval.email ~ Retrieval.hasPredicate(canApprovePredicate)

  def retrievalsForUser(user: UserModel): Retrieval.Username ~ Option[Retrieval.Email] ~ Boolean = {
    uk.gov.hmrc.internalauth.client.~(
      uk.gov.hmrc.internalauth.client.~(
        Retrieval.Username(user.userName),
        user.email.map(Retrieval.Email)
      ),
      user.permissions.canApprove
    )
  }

}
