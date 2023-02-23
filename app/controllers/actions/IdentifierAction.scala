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
import controllers.actions.AuthenticatedIdentifierAction.canApprovePredicate
import models.requests.IdentifierRequest
import models.user.{LdapUser, Permissions, UserModel}
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, IAAction, Predicate, Resource, Retrieval, ~}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject()(val parser: BodyParsers.Default,
                                              auth: FrontendAuthComponents,
                                              config: FrontendAppConfig
                                             )(implicit val executionContext: ExecutionContext) extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    auth.verify(Retrieval.username ~ Retrieval.email ~ Retrieval.hasPredicate(canApprovePredicate)) flatMap {
      case Some(username ~ maybeEmail ~ canApprove) =>
        block(
          IdentifierRequest(
            request,
            UserModel(
              s"LDAP-${username.value}",
              username.value,
              LdapUser,
              maybeEmail.map(email => email.value),
              Permissions(canApprove = canApprove)
            )
          )
        )
      case None =>
        Future.successful(
        Redirect(config.loginUrl, Map("continue_url" -> Seq(config.loginContinueUrl)))
      )
    }
  }

}

object AuthenticatedIdentifierAction {

  val approverResourceType: String = "api-hub-frontend"
  val approverResourceLocation: String = "approvals"
  val approverAction: String = "WRITE"

  private val canApprovePredicate: Predicate = Predicate.Permission(
    resource = Resource.from(resourceType = approverResourceType, resourceLocation = approverResourceLocation),
    action = IAAction(approverAction)
  )

}
