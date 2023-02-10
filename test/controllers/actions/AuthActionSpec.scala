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
import config.FrontendAppConfig
import models.requests.IdentifierRequest
import models.user.{LdapUser, Permissions, UserModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends SpecBase with MockitoSugar {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val mockAuth = mock[FrontendAuthComponents]
          when(mockAuth.verify(any())(any(), any())).thenReturn(Future.successful(None))
          val authAction = new AuthenticatedIdentifierAction(bodyParsers, mockAuth, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must include("/sign-in")
        }
      }
    }

    "when the user has logged in" - {
      "must setup the user when they can approve" in {

        implicit val cc: ControllerComponents = stubMessagesControllerComponents()

        val mockStubBehaviour = mock[StubBehaviour]
        val stubAuth = FrontendAuthComponentsStub(mockStubBehaviour)

        val application = applicationBuilder(userAnswers = None)
          .bindings(
            bind[FrontendAuthComponents].toInstance(stubAuth)
          )
          .build()

        running(application) {

          val authAction = application.injector.instanceOf[AuthenticatedIdentifierAction]

          val canApprovePredicate = Predicate.Permission(
            Resource(
              ResourceType("api-hub-frontend"),
              ResourceLocation("approvals")
            ),
            IAAction("WRITE")
          )

          val expectedRetrieval = Retrieval.username ~ Retrieval.email ~ Retrieval.hasPredicate(canApprovePredicate)

          val testUser = UserModel(
            userId = "LDAP-jo.bloggs",
            userName = "jo.bloggs",
            userType = LdapUser,
            email = Some("jo.bloggs@email.com"),
            permissions = Permissions(canApprove = true)
          )
          when(mockStubBehaviour.stubAuth(ArgumentMatchers.eq(None), ArgumentMatchers.eq(expectedRetrieval)))
            .thenReturn(Future.successful(
              uk.gov.hmrc.internalauth.client.~(
                uk.gov.hmrc.internalauth.client.~(
                  Retrieval.Username(testUser.userName),
                  testUser.email.map(Retrieval.Email)
                ),
                true
              )
            ))

          val result = authAction.invokeBlock(
            FakeRequest().withSession(SessionKeys.authToken -> "Open sesame"),
            (identifierRequest: IdentifierRequest[AnyContent]) => {
              identifierRequest.user mustBe testUser
              Future.successful(Ok)
            }
          )

          status(result) mustBe OK
        }

      }
    }
  }

}
