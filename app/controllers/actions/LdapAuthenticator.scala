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

import com.google.inject.{Inject, Singleton}
import controllers.actions.LdapAuthenticator.{canApprovePredicate, canSupportPredicate, isPrivilegedPredicate}
import models.user.{LdapUser, Permissions, UserModel}
import play.api.mvc.Request
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LdapAuthenticator @Inject()(
  auth: FrontendAuthComponents
)(implicit ec: ExecutionContext) extends Authenticator with FrontendHeaderCarrierProvider {

  def authenticate()(implicit request: Request[?]): Future[UserAuthResult] = {
    auth.verify(
      Retrieval.username ~
        Retrieval.email ~
        Retrieval.hasPredicate(canApprovePredicate) ~
        Retrieval.hasPredicate(canSupportPredicate) ~
        Retrieval.hasPredicate(isPrivilegedPredicate)
    ) flatMap {
      case Some(username ~ maybeEmail ~ canApprove ~ canSupport ~ isPrivileged) =>
        maybeEmail match {
          case Some(email) =>
            Future.successful(UserAuthenticated(
              UserModel(
                buildUserId(username),
                LdapUser,
                email.value,
                Permissions(canApprove = canApprove, canSupport = canSupport, isPrivileged = isPrivileged)
              )
            ))
          case None =>
            Future.successful(UserMissingEmail(buildUserId(username), LdapUser))
        }
      case None =>
        Future.successful(
          UserUnauthenticated
        )
    }
  }

  private def buildUserId(username: Retrieval.Username): String = {
    s"LDAP-${username.value}"
  }

}

object LdapAuthenticator {

  val approverResourceType: String = "api-hub-frontend"
  val approverResourceLocation: String = "approvals"
  val approverAction: String = "WRITE"

  private val canApprovePredicate: Predicate = Predicate.Permission(
    resource = Resource.from(resourceType = approverResourceType, resourceLocation = approverResourceLocation),
    action = IAAction(approverAction)
  )

  val supportResourceType: String = "api-hub-frontend"
  val supportResourceLocation: String = "support"
  val supportAction: String = "WRITE"

  private val canSupportPredicate: Predicate = Predicate.Permission(
    resource = Resource.from(resourceType = supportResourceType, resourceLocation = supportResourceLocation),
    action = IAAction(supportAction)
  )

  val privilegedResourceType: String = "api-hub-frontend"
  val privilegedResourceLocation: String = "privileged-usage"
  val privilegedAction: String = "WRITE"

  private val isPrivilegedPredicate: Predicate = Predicate.Permission(
    resource = Resource.from(resourceType = privilegedResourceType, resourceLocation = privilegedResourceLocation),
    action = IAAction(privilegedAction)
  )

}
