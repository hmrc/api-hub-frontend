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

package controllers

import com.google.inject.{Inject, Singleton}
import controllers.ExperimentalLdapAuth.canApprovePredicate
import controllers.actions.AuthenticatedIdentifierAction.{approverAction, approverResourceLocation, approverResourceType}
import models.user.{LdapUser, Permissions, UserModel}
import play.api.mvc.Request
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExperimentalLdapAuth @Inject()(
  auth: FrontendAuthComponents
)(implicit ec: ExecutionContext) extends FrontendHeaderCarrierProvider {

  def authenticate()(implicit request: Request[_]): Future[UserAuthResult] = {
    auth.verify(Retrieval.username ~ Retrieval.email ~ Retrieval.hasPredicate(canApprovePredicate)) flatMap {
      case Some(username ~ maybeEmail ~ canApprove) =>
        Future.successful(UserAuthenticated(
          UserModel(
            s"LDAP-${username.value}",
            username.value,
            LdapUser,
            maybeEmail.map(email => email.value),
            Permissions(canApprove = canApprove)
          )
        ))
      case None =>
        Future.successful(
          UserUnauthenticated
        )
    }
  }

}

object ExperimentalLdapAuth {

  private val canApprovePredicate: Predicate = Predicate.Permission(
    resource = Resource.from(resourceType = approverResourceType, resourceLocation = approverResourceLocation),
    action = IAAction(approverAction)
  )

}