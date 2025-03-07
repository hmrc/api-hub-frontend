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
import models.hubstatus.{FeatureStatus, FrontendShutter}
import models.requests.OptionalIdentifierRequest
import models.user.UserModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import play.api.mvc.Results.ServiceUnavailable
import services.HubStatusService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import views.html.ShutteredView

import scala.concurrent.{ExecutionContext, Future}

trait OptionalIdentifierAction
  extends ActionBuilder[OptionalIdentifierRequest, AnyContent]
    with ActionFunction[Request, OptionalIdentifierRequest]

class OptionallyAuthenticatedIdentifierAction @Inject()(
  val parser: BodyParsers.Default,
  override val messagesApi: MessagesApi,
  ldapAuthenticator: LdapAuthenticator,
  strideAuthenticator: StrideAuthenticator,
  hubStatusService: HubStatusService,
  shutteredView: ShutteredView,
  frontendAppConfig: FrontendAppConfig
)(implicit val executionContext: ExecutionContext) extends OptionalIdentifierAction with FrontendHeaderCarrierProvider with I18nSupport {

 override def invokeBlock[A](request: Request[A], block: OptionalIdentifierRequest[A] => Future[Result]): Future[Result] = {
    if (hc(request).authorization.isDefined) {
      for {
        featureStatus <- hubStatusService.status(FrontendShutter)
        strideAuthResult <- strideAuthenticator.authenticate()(request)
        authResult <- strideAuthResult match {
          case UserUnauthenticated => ldapAuthenticator.authenticate()(request)
          case result: UserAuthResult => Future.successful(result)
        }
        result <- authResult match {
          case UserAuthenticated(user) => processRequest(request, block, Some(user), featureStatus)
          case UserMissingEmail(userId, userType) => processRequest(request, block, None, featureStatus)
          case UserUnauthorised => processRequest(request, block, None, featureStatus)
          case UserUnauthenticated => processRequest(request, block, None, featureStatus)
        }
      } yield result
    }
    else {
      block(OptionalIdentifierRequest(request, None))
    }
  }

  private def processRequest[A](
    request: Request[A],
    block: OptionalIdentifierRequest[A] => Future[Result],
    user: Option[UserModel],
    featureStatus: FeatureStatus
  ): Future[Result] = {
    if (featureStatus.available || user.exists(_.permissions.canSupport)) {
      block(OptionalIdentifierRequest(request, user))
    }
    else {
      shuttered(featureStatus, user)(request)
    }
  }

  private def shuttered(featureStatus: FeatureStatus, user: Option[UserModel])(implicit request: Request[?]): Future[Result] = {
    val shutterMessage = featureStatus.shutterMessage match {
      case Some(shutterMessage) => shutterMessage
      case None => frontendAppConfig.shutterMessage
    }

    Future.successful(ServiceUnavailable(shutteredView(shutterMessage, user)))
  }

}
