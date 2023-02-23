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

package models.errors

import play.api.mvc.{Call, Result}
import play.api.mvc.Results.{NotFound, Redirect}

import scala.concurrent.{ExecutionContext, Future}

trait ErrorResponseHandling {

  def redirectNotFoundOrThrow[T]
      (result: Future[Either[RequestError, Option[T]]], redirectCall: T => Call)
      (implicit ec: ExecutionContext): Future[Result] = {
    result.flatMap {
      case Right(Some(t)) => Future.successful(Redirect(redirectCall.apply(t)))
      case Right(None) => Future.successful(NotFound)
      case Left(e) => Future.failed(new RuntimeException())
    }
  }

  def redirectOrThrow[T]
      (result: Future[Either[RequestError, T]], redirectCall: T => Call)
      (implicit ec: ExecutionContext): Future[Result] = {
    result.flatMap {
      case Right(t) => Future.successful(Redirect(redirectCall.apply(t)))
      case Left(e) => Future.failed(new RuntimeException())
    }
  }

}
