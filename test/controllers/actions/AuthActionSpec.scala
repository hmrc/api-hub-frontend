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
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{Application, Configuration, Environment}
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.test.FrontendAuthComponentsStub

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.internalauth.client.AuthFunctions
class AuthActionSpec extends SpecBase {

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
          val mockAuthFunctions = mock[AuthFunctions]
          when(mockAuthFunctions.verify(any())(any(), any())).thenThrow(UpstreamErrorResponse.apply("bad thing", 401))
          val authAction = new AuthenticatedIdentifierAction(bodyParsers, mockAuthFunctions, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith("/stride/sign-in")
        }
      }
    }

    //    "the user's session has expired" - {
    //
    //      "must redirect the user to log in " in {
    //
    //        val application = applicationBuilder(userAnswers = None).build()
    //
    //        running(application) {
    //          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
    //          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
    //          val configuration = application.injector.instanceOf[Configuration]
    //          val env = application.injector.instanceOf[Environment]
    //
    //          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, bodyParsers, configuration, env)
    //          val controller = new Harness(authAction)
    //          val result = controller.onPageLoad()(FakeRequest())
    //
    //          status(result) mustBe SEE_OTHER
    //          redirectLocation(result).value must startWith("/stride/sign-in")
    //        }
    //      }
    //    }
    //
    //    "the user doesn't have sufficient enrolments" - {
    //
    //      "must redirect the user to the unauthorised page" in {
    //
    //        val application = applicationBuilder(userAnswers = None).build()
    //
    //        running(application) {
    //          val authAction = buildAuthenticatedIdentifierAction(application)
    //          val controller = new Harness(authAction)
    //          val result = controller.onPageLoad()(FakeRequest())
    //
    //          status(result) mustBe SEE_OTHER
    //          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
    //        }
    //      }
    //    }
    //
    //    "the user doesn't have sufficient confidence level" - {
    //
    //      "must redirect the user to the unauthorised page" in {
    //
    //        val application = applicationBuilder(userAnswers = None).build()
    //
    //        running(application) {
    //          val authAction = buildAuthenticatedIdentifierAction(application)
    //          val controller = new Harness(authAction)
    //          val result = controller.onPageLoad()(FakeRequest())
    //
    //          status(result) mustBe SEE_OTHER
    //          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
    //        }
    //      }
    //    }
    //
    //    "the user used an unaccepted auth provider" - {
    //
    //      "must redirect the user to the unauthorised page" in {
    //
    //        val application = applicationBuilder(userAnswers = None).build()
    //
    //        running(application) {
    //          val authAction = buildAuthenticatedIdentifierAction(application)
    //          val controller = new Harness(authAction)
    //          val result = controller.onPageLoad()(FakeRequest())
    //
    //          status(result) mustBe SEE_OTHER
    //          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
    //        }
    //      }
    //    }
    //
    //    "the user has an unsupported affinity group" - {
    //
    //      "must redirect the user to the unauthorised page" in {
    //
    //        val application = applicationBuilder(userAnswers = None).build()
    //
    //        running(application) {
    //          val authAction = buildAuthenticatedIdentifierAction(application)
    //          val controller = new Harness(authAction)
    //          val result = controller.onPageLoad()(FakeRequest())
    //
    //          status(result) mustBe SEE_OTHER
    //          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    //        }
    //      }
    //    }
    //
    //    "the user has an unsupported credential role" - {
    //
    //      "must redirect the user to the unauthorised page" in {
    //
    //        val application = applicationBuilder(userAnswers = None).build()
    //
    //        running(application) {
    //          val authAction = buildAuthenticatedIdentifierAction(application)
    //          val controller = new Harness(authAction)
    //          val result = controller.onPageLoad()(FakeRequest())
    //
    //          status(result) mustBe SEE_OTHER
    //          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    //        }
    //      }
    //    }
    //  }
    //}
    //
    //object AuthActionSpec {
    //
    //  def buildAuthenticatedIdentifierAction(application: Application): AuthenticatedIdentifierAction = {
    //    val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
    //    val appConfig   = application.injector.instanceOf[FrontendAppConfig]
    //    val configuration = application.injector.instanceOf[Configuration]
    //    val env = application.injector.instanceOf[Environment]
    //
    //    new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, bodyParsers, configuration, env)
    //  }
    //
    //}
    //
    //class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
    //  val serviceUrl: String = ""
    //
    //  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    //    Future.failed(exceptionToReturn)
    //}
  }
}