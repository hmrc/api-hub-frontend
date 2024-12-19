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

package controllers.helpers

import com.google.inject.{Inject, Singleton}
import models.api.ApiDetail
import models.application.Application
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Request, Result, WrappedRequest}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import views.html.ErrorTemplate

@Singleton
class ErrorResultBuilder @Inject()(
  errorTemplate: ErrorTemplate,
  override val messagesApi: MessagesApi
) extends I18nSupport with Logging {

  def badRequest()(implicit request: Request[?]): Result = {
    badRequest(None, None, None)
  }

  def badRequest(message: String)(implicit request: Request[?]): Result = {
    badRequest(None, None, Some(message))
  }

  def badRequest(heading: String, message: String)(implicit request: Request[?]): Result = {
    badRequest(None, Some(heading), Some(message))
  }

  def badRequest(title: String, heading: String, message: String)(implicit request: Request[?]): Result = {
    badRequest(Some(title), Some(heading), Some(message))
  }

  private def badRequest(title: Option[String], heading: Option[String], message: Option[String])(implicit request: Request[?]): Result = {
    BadRequest(
      logAndBuildErrorTemplate(
        BAD_REQUEST,
        title.getOrElse(Messages("global.error.badRequest400.title")),
        heading.getOrElse(Messages("global.error.badRequest400.heading")),
        message.getOrElse(Messages("global.error.badRequest400.message"))
      )
    )
  }

  def notFound()(implicit request: Request[?]): Result = {
    notFound(None, None, None)
  }

  def notFound(message: String)(implicit request: Request[?]): Result = {
    notFound(None, None, Some(message))
  }

  def notFound(heading: String, message: String)(implicit request: Request[?]): Result = {
    notFound(None, Some(heading), Some(message))
  }

  def applicationNotFound(id: String)(implicit request: Request[?]): Result = {
    notFound(
      Messages("site.applicationNotFoundHeading"),
      Messages("site.applicationNotFoundMessage", id)
    )
  }

  def apiNotFound(id: String)(implicit request: Request[?]): Result = {
    notFound(
      Messages("site.apiNotFound.heading"),
      Messages("site.apiNotFound.message", id)
    )
  }

  def apiNotFoundInApim(apiDetail: ApiDetail)(implicit request: Request[?]): Result = {
    notFound(
      Messages("site.apiNotFound.heading"),
      Messages("site.apiNotFoundInApim.message", apiDetail.title)
    )
  }

  def apiNotFoundInApplication(apiTitle: String, application: Application)(implicit request: Request[?]): Result = {
    notFound(
      Messages("site.apiNotFound.heading"),
      Messages("site.apiNotFoundInApplication.message", apiTitle, application.name)
    )
  }

  def apiNotFoundInApplication(apiDetail: ApiDetail, application: Application)(implicit request: Request[?]): Result = {
    apiNotFoundInApplication(apiDetail.title, application)
  }

  def accessRequestNotFound(id: String)(implicit request: Request[?]): Result = {
    notFound(
      Messages("site.accessRequestNotFound.heading"),
      Messages("site.accessRequestNotFound.message", id)
    )
  }

  def teamNotFound(teamId: String)(implicit request: Request[?]): Result = {
    notFound(
      Messages("site.teamNotFoundHeading"),
      Messages("site.teamNotFoundMessage", teamId)
    )
  }

  def environmentNotFound(environment: String)(implicit request: Request[?]): Result =
    notFound(
      heading = Messages("site.environmentNotFoundHeading"),
      message = Messages("site.environmentNotFoundMessage", environment)
    )

  def notFound(title: String, heading: String, message: String)(implicit request: Request[?]): Result = {
    notFound(Some(title), Some(heading), Some(message))
  }

  private def notFound(title: Option[String], heading: Option[String], message: Option[String])(implicit request: Request[?]): Result = {
    NotFound(
      logAndBuildErrorTemplate(
        NOT_FOUND,
        title.getOrElse(Messages("global.error.pageNotFound404.title")),
        heading.getOrElse(Messages("global.error.pageNotFound404.heading")),
        message.getOrElse(Messages("global.error.pageNotFound404.message"))
      )
    )
  }

  def internalServerError(error: String)(implicit request: Request[?]): Result = {
    logger.warn(s"Internal server error: $error")
    internalServerError()
  }

  def internalServerError(t: Throwable)(implicit request: Request[?]): Result = {
    logger.warn("Internal server error", t)
    internalServerError()
  }

  def internalServerError(message: String, t: Throwable)(implicit request: Request[?]): Result = {
    logger.warn("Internal server error", t)
    internalServerError(Some(message))
  }

  private def internalServerError(message: Option[String] = None)(implicit request: Request[?]): Result = {
    InternalServerError(
      logAndBuildErrorTemplate(
        INTERNAL_SERVER_ERROR,
        Messages("global.error.InternalServerError500.title"),
        Messages("global.error.InternalServerError500.heading"),
        message.getOrElse(Messages("global.error.InternalServerError500.message"))
      )
    )
  }

  private def logAndBuildErrorTemplate(status: Int, title: String, heading: String, message: String)(implicit request: Request[?]) = {
    logger.warn(s"Responding with error status $status. Title: $title Heading: $heading Message: $message")

    errorTemplate(
      title,
      heading,
      message,
      IdentifierRequest.extractUserFromRequest(request)
    )
  }

}
