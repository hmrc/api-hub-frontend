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
import controllers.actions.StrideAuthenticator.{API_HUB_APPROVER_ROLE, API_HUB_PRIVILEGED_USER_ROLE, API_HUB_SUPPORT_ROLE, API_HUB_USER_ROLE}
import models.user.{Permissions, StrideUser, UserModel}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, Enrolment, InsufficientEnrolments, NoActiveSession}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StrideAuthenticator @Inject()(
  override val authConnector: AuthConnector
)(implicit ec: ExecutionContext) extends Authenticator with AuthorisedFunctions with FrontendHeaderCarrierProvider {

  def authenticate()(implicit request: Request[?]): Future[UserAuthResult] = {
    authorised(
      Enrolment(API_HUB_USER_ROLE) or
        Enrolment(API_HUB_APPROVER_ROLE) or
        Enrolment(API_HUB_SUPPORT_ROLE) or
        Enrolment(API_HUB_PRIVILEGED_USER_ROLE) and
        AuthProviders(PrivilegedApplication)
    )
      .retrieve(Retrievals.authorisedEnrolments and Retrievals.email and Retrievals.credentials) {
        case authorisedEnrolments ~ email ~ credentials =>
          (email, credentials.map(_.providerId)) match {
            case (Some(email), Some(providerId)) if email.trim.nonEmpty =>
              Future.successful(UserAuthenticated(
                UserModel(
                  userId = s"STRIDE-$providerId",
                  userType = StrideUser,
                  email = email.trim,
                  permissions = Permissions(
                    canApprove = authorisedEnrolments.enrolments.exists(enrolment => enrolment.key.equals(API_HUB_APPROVER_ROLE)),
                    canSupport = authorisedEnrolments.enrolments.exists(enrolment => enrolment.key.equals(API_HUB_SUPPORT_ROLE)),
                    isPrivileged = authorisedEnrolments.enrolments.exists(enrolment => enrolment.key.equals(API_HUB_PRIVILEGED_USER_ROLE))
                  )
                )
              ))
            case (_, None) =>
              Future.failed(new UnauthorizedException("Unable to retrieve Stride provider Id"))
            case _ =>
              Future.successful(UserMissingEmail(StrideUser))
          }
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
  val API_HUB_SUPPORT_ROLE: String = "api_hub_support"
  val API_HUB_PRIVILEGED_USER_ROLE: String = "api_hub_privileged_user"

}
