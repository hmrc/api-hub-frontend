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

package fakes

import controllers.helpers.ErrorResultBuilder
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.mvc.{Request, Result}

object FakeErrorResultBuilder extends ErrorResultBuilder {

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

  private def badRequest(title: Option[String], heading: Option[String], message: Option[String]): Result = {
    BadRequest(
      errorDescription(
        title.getOrElse("test-bad-request-title"),
        heading.getOrElse("test-bad-request-heading"),
        message.getOrElse("test-bad-request-message")
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

  def notFound(title: Option[String], heading: Option[String], message: Option[String]): Result = {
    NotFound(
      errorDescription(
        title.getOrElse("test-not-found-title"),
        heading.getOrElse("test-not-found-heading"),
        message.getOrElse("test-not-found-message")
      )
    )
  }

  def internalServerError(error: String)(implicit request: Request[_]): Result = {
    internalServerError()
  }

  def internalServerError(t: Throwable)(implicit request: Request[_]): Result = {
    internalServerError()
  }

  private def internalServerError(): Result = {
    InternalServerError(
      errorDescription(
        "test-internal-server-error-title",
        "test-internal-server-error-heading",
        "test-internal-server-error-message"
      )
    )
  }

  private def errorDescription(title: String, heading: String, message: String): String = {
    s"Title: $title heading: $heading message: $message"
  }

}
