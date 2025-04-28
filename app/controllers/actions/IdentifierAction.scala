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
import config.FrontendAppConfig
import handlers.ErrorHandler
import models.application.Api
import models.hubstatus.{FeatureStatus, FrontendShutter}
import models.requests.IdentifierRequest
import models.user.{LdapUser, StrideUser, UserModel, UserType}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.*
import play.api.mvc.*
import play.twirl.api.Html
import services.HubStatusService
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.ShutteredView

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject()(
  val parser: BodyParsers.Default,
  override val messagesApi: MessagesApi,
  ldapAuthenticator: LdapAuthenticator,
  strideAuthenticator: StrideAuthenticator,
  crypto: ApplicationCrypto,
  hubStatusService: HubStatusService,
  errorHandler: ErrorHandler,
  shutteredView: ShutteredView,
  frontendAppConfig: FrontendAppConfig
)(implicit val executionContext: ExecutionContext) extends IdentifierAction with Logging with I18nSupport {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    for {
      featureStatus <- hubStatusService.status(FrontendShutter)
      strideAuthResult <- strideAuthenticator.authenticate()(request)
      authResult <- strideAuthResult match {
        case UserUnauthenticated => ldapAuthenticator.authenticate()(request)
        case result: UserAuthResult => Future.successful(result)
      }
      result <- authResult match {
        case UserAuthenticated(user) => authenticated(request, block, user, featureStatus)
        case UserMissingEmail(userId, userType) => handleMissingEmail(featureStatus, userId, userType)(request)
        case UserUnauthorised => unauthorized(featureStatus)(request)
        case UserUnauthenticated => unauthenticated(featureStatus)(request)
      }
    } yield result
  }

  private def authenticated[A](
    request: Request[A],
    block: IdentifierRequest[A] => Future[Result],
    user: UserModel,
    featureStatus: FeatureStatus
  ): Future[Result] = {
    if (featureStatus.available || user.permissions.canSupport) {
      val encryptedEmail = crypto.QueryParameterCrypto.encrypt(PlainText(user.email)).value
      val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request).withExtraHeaders(("Encrypted-User-Email", encryptedEmail))
      block(IdentifierRequest(request, user, hc))
    }
    else {
      shuttered(featureStatus, Some(user))(request)
    }
  }

  private def unauthorized(featureStatus: FeatureStatus)(implicit request: Request[?]): Future[Result] = {
    if (featureStatus.available) {
      Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
    }
    else {
      shuttered(featureStatus, None)
    }
  }

  private def unauthenticated(featureStatus: FeatureStatus)(implicit request: Request[?]): Future[Result] = {
    if (featureStatus.available) {
      Future.successful(Redirect(controllers.auth.routes.SignInController.onPageLoad()))
    }
    else {
      shuttered(featureStatus, None)
    }
  }

  private def shuttered(featureStatus: FeatureStatus, user: Option[UserModel])(implicit request: Request[?]): Future[Result] = {
    val shutterMessage = featureStatus.shutterMessage match {
      case Some(shutterMessage) => shutterMessage
      case None => frontendAppConfig.shutterMessage
    }

    Future.successful(ServiceUnavailable(shutteredView(shutterMessage, user)))
  }

  private def handleMissingEmail(featureStatus: FeatureStatus, userId: String, userType: UserType)(implicit request: Request[?]): Future[Result] = {
    if (featureStatus.available) {
      logger.warn(s"Missing email address for user withId $userId of type $userType")
      buildMissingEmailView(userType).map(html => Ok(html))
    }
    else {
      shuttered(featureStatus, None)
    }
  }

  private def buildMissingEmailView(userType: UserType)(implicit request: RequestHeader): Future[Html] = {
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
