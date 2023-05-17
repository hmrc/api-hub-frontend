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
import controllers.actions.StrideAuthenticator.{API_HUB_ADMINISTRATOR_ROLE, API_HUB_APPROVER_ROLE, API_HUB_USER_ROLE}
import models.user.{Permissions, StrideUser, UserModel}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, Enrolment, InsufficientEnrolments, NoActiveSession}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StrideAuthenticator @Inject()(
  override val authConnector: AuthConnector
)(implicit ec: ExecutionContext) extends Authenticator with AuthorisedFunctions with FrontendHeaderCarrierProvider {

  def authenticate()(implicit request: Request[_]): Future[UserAuthResult] = {
    authorised(
      Enrolment(API_HUB_USER_ROLE) or
        Enrolment(API_HUB_APPROVER_ROLE) or
        Enrolment(API_HUB_ADMINISTRATOR_ROLE) and
        AuthProviders(PrivilegedApplication)
    )
      .retrieve(Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.email and Retrievals.credentials) {
        case authorisedEnrolments ~ name ~ email ~ credentials =>
          Future.successful(UserAuthenticated(
            UserModel(
              userId = s"STRIDE-${credentials.map(_.providerId).getOrElse(name)}",
              userName = name.map(_.name.getOrElse("")).getOrElse(""),
              userType = StrideUser,
              email = email,
              permissions = Permissions(
                canApprove = authorisedEnrolments.enrolments.exists(enrolment => enrolment.key.equals(API_HUB_APPROVER_ROLE)),
                canAdminister = authorisedEnrolments.enrolments.exists(enrolment => enrolment.key.equals(API_HUB_ADMINISTRATOR_ROLE))
              )
            )
          ))
      }.recover {
      case _: NoActiveSession =>
        UserUnauthenticated
      case _: InsufficientEnrolments =>
        UserUnauthorised
    }
  }

}

object StrideAuthenticator {

  val API_HUB_USER_ROLE: String = "api_hub_user"
  val API_HUB_APPROVER_ROLE: String = "api_hub_approver"
  val API_HUB_ADMINISTRATOR_ROLE: String = "api_hub_administrator"

}
