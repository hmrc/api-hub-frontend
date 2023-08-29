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
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import views.html.ErrorTemplate

trait ErrorResultBuilder {

  def badRequest()(implicit request: Request[_]): Result

  def badRequest(message: String)(implicit request: Request[_]): Result

  def badRequest(heading: String, message: String)(implicit request: Request[_]): Result

  def badRequest(title: String, heading: String, message: String)(implicit request: Request[_]): Result

  def notFound()(implicit request: Request[_]): Result

  def notFound(message: String)(implicit request: Request[_]): Result

  def notFound(heading: String, message: String)(implicit request: Request[_]): Result

  def notFound(title: String, heading: String, message: String)(implicit request: Request[_]): Result

  def internalServerError(error: String)(implicit request: Request[_]): Result

  def internalServerError(t: Throwable)(implicit request: Request[_]): Result

}

@Singleton
class ErrorResultBuilderImpl @Inject()(
  errorTemplate: ErrorTemplate,
  override val messagesApi: MessagesApi
) extends ErrorResultBuilder with I18nSupport with Logging {

  def badRequest()(implicit request: Request[_]): Result = {
    badRequest(None, None, None)
  }

  def badRequest(message: String)(implicit request: Request[_]): Result = {
    badRequest(None, None, Some(message))
  }

  def badRequest(heading: String, message: String)(implicit request: Request[_]): Result = {
    badRequest(None, Some(heading), Some(message))
  }

  def badRequest(title: String, heading: String, message: String)(implicit request: Request[_]): Result = {
    badRequest(Some(title), Some(heading), Some(message))
  }

  private def badRequest(title: Option[String], heading: Option[String], message: Option[String])(implicit request: Request[_]): Result = {
    BadRequest(
      logAndBuildErrorTemplate(
        BAD_REQUEST,
        title.getOrElse(Messages("global.error.badRequest400.title")),
        heading.getOrElse(Messages("global.error.badRequest400.heading")),
        message.getOrElse(Messages("global.error.badRequest400.message"))
      )
    )
  }

  def notFound()(implicit request: Request[_]): Result = {
    notFound(None, None, None)
  }

  def notFound(message: String)(implicit request: Request[_]): Result = {
    notFound(None, None, Some(message))
  }

  def notFound(heading: String, message: String)(implicit request: Request[_]): Result = {
    notFound(None, Some(heading), Some(message))
  }

  def notFound(title: String, heading: String, message: String)(implicit request: Request[_]): Result = {
    notFound(Some(title), Some(heading), Some(message))
  }

  private def notFound(title: Option[String], heading: Option[String], message: Option[String])(implicit request: Request[_]): Result = {
    NotFound(
      logAndBuildErrorTemplate(
        NOT_FOUND,
        title.getOrElse(Messages("global.error.pageNotFound404.title")),
        heading.getOrElse(Messages("global.error.pageNotFound404.heading")),
        message.getOrElse(Messages("global.error.pageNotFound404.message"))
      )
    )
  }

  def internalServerError(error: String)(implicit request: Request[_]): Result = {
    internalServerError()
  }

  def internalServerError(t: Throwable)(implicit request: Request[_]): Result = {
    internalServerError()
  }

  private def internalServerError()(implicit request: Request[_]): Result = {
    InternalServerError(
      logAndBuildErrorTemplate(
        INTERNAL_SERVER_ERROR,
        Messages("global.error.InternalServerError500.title"),
        Messages("global.error.InternalServerError500.heading"),
        Messages("global.error.InternalServerError500.message")
      )
    )
  }

  private def logAndBuildErrorTemplate(status: Int, title: String, heading: String, message: String)(implicit request: Request[_]) = {
    logger.warn(s"Responding with error status $status. Title: $title Heading: $heading Message: $message")

    errorTemplate(
      title,
      heading,
      message
    )
  }

}
