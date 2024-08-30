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
import handlers.ErrorHandler
import models.requests.IdentifierRequest
import models.user.{LdapUser, StrideUser, UserModel, UserType}
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject()(
  val parser: BodyParsers.Default,
  ldapAuthenticator: LdapAuthenticator,
  strideAuthenticator: StrideAuthenticator,
  errorHandler: ErrorHandler
)(implicit val executionContext: ExecutionContext) extends IdentifierAction with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    strideAuthenticator.authenticate()(request).flatMap {
      case UserUnauthenticated => ldapAuthenticator.authenticate()(request)
      case result: UserAuthResult => Future.successful(result)
    }.flatMap {
      case UserAuthenticated(user) => block(IdentifierRequest(request, user))
      case UserMissingEmail(userType) => handleMissingEmail(userType)(request)
      case UserUnauthorised => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
      case UserUnauthenticated => Future.successful(Redirect(controllers.auth.routes.SignInController.onPageLoad()))
    }
  }

  private def handleMissingEmail(userType: UserType)(implicit request: RequestHeader) = {
    logger.warn(s"Missing email address for user of type $userType")
    buildMissingEmailView(userType).map(html => Ok(html))
  }

  private def buildMissingEmailView(userType: UserType)(implicit request: RequestHeader) = {
    val messages = errorHandler.messagesApi.messages.getOrElse("en", Map.empty)
    val title = messages.getOrElse("unauthorised.title", "")
    val heading = messages.getOrElse("unauthorised.missingEmail.heading", "")
    userType match {
      case StrideUser =>
        errorHandler.standardErrorTemplate(
          title,
          heading,
          messages.getOrElse("unauthorised.missingEmail.stride", ""))
      case LdapUser =>
        errorHandler.standardErrorTemplate(
          title,
          heading,
          messages.getOrElse("unauthorised.missingEmail.ldap", ""))
    }
  }

}
