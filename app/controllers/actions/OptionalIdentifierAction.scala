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

import com.google.inject.Inject
import models.requests.OptionalIdentifierRequest
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait OptionalIdentifierAction
  extends ActionBuilder[OptionalIdentifierRequest, AnyContent]
    with ActionFunction[Request, OptionalIdentifierRequest]

class OptionallyAuthenticatedIdentifierAction @Inject()(
  val parser: BodyParsers.Default,
  ldapAuthenticator: LdapAuthenticator,
  strideAuthenticator: StrideAuthenticator
)(implicit val executionContext: ExecutionContext) extends OptionalIdentifierAction with FrontendHeaderCarrierProvider {

  override def invokeBlock[A](request: Request[A], block: OptionalIdentifierRequest[A] => Future[Result]): Future[Result] = {
    if (hc(request).authorization.isDefined) {
      strideAuthenticator.authenticate()(request).flatMap {
        case UserUnauthenticated => ldapAuthenticator.authenticate()(request)
        case result: UserAuthResult => Future.successful(result)
      }.flatMap {
        case UserAuthenticated(user) => block(OptionalIdentifierRequest(request, Some(user)))
        case UserUnauthorised => block(OptionalIdentifierRequest(request, None))
        case UserUnauthenticated => block(OptionalIdentifierRequest(request, None))
      }
    }
    else {
      block(OptionalIdentifierRequest(request, None))
    }
  }

}
