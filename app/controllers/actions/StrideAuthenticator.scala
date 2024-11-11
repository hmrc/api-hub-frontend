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
import controllers.actions.StrideAuthenticator.{API_HUB_APPROVER_ROLE, API_HUB_PRIVILEGED_USER_ROLE, API_HUB_SUPPORT_ROLE, API_HUB_USER_ROLE, approverRoles, privilegedRoles, supportRoles, userRoles}
import models.user.{Permissions, StrideUser, UserModel}
import play.api.mvc.Request
import services.MetricsService
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, Enrolment, InsufficientEnrolments, NoActiveSession}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StrideAuthenticator @Inject()(
  override val authConnector: AuthConnector,
  metricsService: MetricsService
)(implicit ec: ExecutionContext) extends Authenticator with AuthorisedFunctions with FrontendHeaderCarrierProvider {

  def authenticate()(implicit request: Request[?]): Future[UserAuthResult] = {
    authorised(
      (approverRoles ++ privilegedRoles ++ supportRoles ++ userRoles)
        .foldRight[Predicate](EmptyPredicate)(Enrolment(_) or _)
          and AuthProviders(PrivilegedApplication)
    )
      .retrieve(Retrievals.authorisedEnrolments and Retrievals.email and Retrievals.credentials) {
        case authorisedEnrolments ~ email ~ credentials =>
          (email, credentials.map(_.providerId)) match {
            case (Some(email), Some(providerId)) if email.trim.nonEmpty =>
              Future.successful(UserAuthenticated(
                UserModel(
                  userId = buildUserId(providerId),
                  userType = StrideUser,
                  email = email.trim,
                  permissions = Permissions(
                    canApprove = authorisedEnrolments.enrolments.exists(enrolment => approverRoles.contains(enrolment.key)),
                    canSupport = authorisedEnrolments.enrolments.exists(enrolment => supportRoles.contains(enrolment.key)),
                    isPrivileged = authorisedEnrolments.enrolments.exists(enrolment => privilegedRoles.contains(enrolment.key))
                  )
                )
              ))
            case (_, None) =>
              Future.failed(new UnauthorizedException("Unable to retrieve Stride provider Id"))
            case (_, Some(providerId)) =>
              metricsService.strideMissingEmail()
              Future.successful(UserMissingEmail(buildUserId(providerId), StrideUser))
          }
      }.recover {
      case _: NoActiveSession =>
        UserUnauthenticated
      case _: InsufficientEnrolments =>
        UserUnauthorised
    }
  }

  private def buildUserId(providerId: String): String = {
    s"STRIDE-$providerId"
  }

}

object StrideAuthenticator {

  val API_HUB_USER_ROLE: String = "api_hub_user"
  val API_HUB_APPROVER_ROLE: String = "api_hub_approver"
  val API_HUB_SUPPORT_ROLE: String = "api_hub_support"
  val API_HUB_PRIVILEGED_USER_ROLE: String = "api_hub_privileged_user"

  val IPAAS_LIVE_SERVICE: String = "ipaas_live_service"
  val IPAAS_LIVE_ADMINS: String = "ipaas_live_admins"
  val IPAAS_LIVE_SERVICE_SC: String = "ipaas_live_service_sc"

  val supportRoles: Seq[String] = Seq(API_HUB_SUPPORT_ROLE, IPAAS_LIVE_ADMINS)
  val approverRoles: Seq[String] = Seq(API_HUB_APPROVER_ROLE, IPAAS_LIVE_ADMINS)
  val privilegedRoles: Seq[String] = Seq(API_HUB_PRIVILEGED_USER_ROLE, IPAAS_LIVE_SERVICE_SC)
  val userRoles: Seq[String] = Seq(API_HUB_USER_ROLE, IPAAS_LIVE_SERVICE)

}
