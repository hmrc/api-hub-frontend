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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ExperimentalStrideLoginView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExperimentalStrideLoginController @Inject()(
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  view: ExperimentalStrideLoginView,
  override val authConnector: AuthConnector,
  override val config: Configuration,
  override val env: Environment
)(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AuthorisedFunctions with AuthRedirects with Logging {

  def noRole: Action[AnyContent] = Action.async {
    implicit request =>
      logger.info("Authorising")
      authorised(AuthProviders(PrivilegedApplication))
        .retrieve(Retrievals.allEnrolments and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.email and Retrievals.credentials) {
          case allEnrolments ~ authorisedEnrolments ~ name ~ email ~ credentials =>
            Future.successful(
              Ok(
                view(
                  ExperimentalStrideLoginViewModel(
                    description = "Test login with API Hub User role required",
                    allEnrolments = allEnrolments,
                    authorisedEnrolments = authorisedEnrolments,
                    name = name,
                    email = email,
                    credentials = credentials
                  )
                )
              )
            )
        }.recover {
        case _: NoActiveSession =>
          logger.warn("NoActiveSession")
          toStrideLogin(routes.ExperimentalStrideLoginController.noRole().absoluteURL())
        case _: InsufficientEnrolments =>
          logger.warn("InsufficientEnrolments")
          SeeOther(routes.UnauthorisedController.onPageLoad.url)
        case t: Throwable =>
          logger.error("Throwable", t)
          throw t
      }
  }

  def apiHubUser: Action[AnyContent] = Action.async {
    implicit request =>
      logger.info("Authorising")
      authorised(Enrolment("api_hub_user") and AuthProviders(PrivilegedApplication))
        .retrieve(Retrievals.allEnrolments and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.email and Retrievals.credentials) {
          case allEnrolments ~ authorisedEnrolments ~ name ~ email ~ credentials =>
            Future.successful(
              Ok(
                view(
                  ExperimentalStrideLoginViewModel(
                    description = "Test login with API Hub User role required",
                    allEnrolments = allEnrolments,
                    authorisedEnrolments = authorisedEnrolments,
                    name = name,
                    email = email,
                    credentials = credentials
                  )
                )
              )
            )
        }.recover {
          case _: NoActiveSession =>
            logger.warn("NoActiveSession")
            toStrideLogin(routes.ExperimentalStrideLoginController.apiHubUser().absoluteURL())
          case _: InsufficientEnrolments =>
            logger.warn("InsufficientEnrolments")
            SeeOther(routes.UnauthorisedController.onPageLoad.url)
          case t: Throwable =>
            logger.error("Throwable", t)
            throw t
        }
  }

  def apiHubApprover: Action[AnyContent] = Action.async {
    implicit request =>
      logger.info("Authorising")
      authorised(Enrolment("api_hub_approver") and AuthProviders(PrivilegedApplication))
        .retrieve(Retrievals.allEnrolments and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.email and Retrievals.credentials) {
          case allEnrolments ~ authorisedEnrolments ~ name ~ email ~ credentials =>
            Future.successful(
              Ok(
                view(
                  ExperimentalStrideLoginViewModel(
                    description = "Test login with API Hub Approver role required",
                    allEnrolments = allEnrolments,
                    authorisedEnrolments = authorisedEnrolments,
                    name = name,
                    email = email,
                    credentials = credentials
                  )
                )
              )
            )
        }.recover {
        case _: NoActiveSession =>
          logger.warn("NoActiveSession")
          toStrideLogin(routes.ExperimentalStrideLoginController.apiHubApprover().absoluteURL())
        case _: InsufficientEnrolments =>
          logger.warn("InsufficientEnrolments")
          SeeOther(routes.UnauthorisedController.onPageLoad.url)
        case t: Throwable =>
          logger.error("Throwable", t)
          throw t
      }
  }

}

case class ExperimentalStrideLoginViewModel(
  description: String,
  allEnrolments: Enrolments,
  authorisedEnrolments: Enrolments,
  name: Option[Name],
  email: Option[String],
  credentials: Option[Credentials]
)
