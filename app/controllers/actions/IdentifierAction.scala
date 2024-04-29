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
      case UserAuthenticated(user) if !user.email.exists(_.trim.nonEmpty) => handleMissingEmail(user)(request)
      case UserAuthenticated(user) => block(IdentifierRequest(request, user))
      case UserUnauthorised => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
      case UserUnauthenticated => Future.successful(Redirect(controllers.auth.routes.SignInController.onPageLoad()))
    }
  }

  private def handleMissingEmail(user: UserModel)(implicit request: Request[_]) = {
    logger.warn(s"Missing email address for user ${user.userName} with id ${user.userId}")
    Future.successful(Ok(buildMissingEmailView(user.userType)))
  }

  private def buildMissingEmailView(userType: UserType)(implicit request: Request[_]) = {
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
