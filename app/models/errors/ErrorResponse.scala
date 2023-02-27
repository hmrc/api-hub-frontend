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

import models.Enumerable
import play.api.libs.json.{Format, Json}

sealed trait RequestError

trait UserRequestError extends RequestError
case object TeamMemberAlreadyExists extends UserRequestError

trait ServiceRequestError extends RequestError

case object InvalidJson extends ServiceRequestError

case object ApplicationNameNotUnique extends RequestError

object RequestError extends Enumerable.Implicits {

  val values: Seq[RequestError] = Seq(
    InvalidJson,
    ApplicationNameNotUnique,
    TeamMemberAlreadyExists
  )

  implicit val enumerable: Enumerable[RequestError] =
    Enumerable(values.map(value => value.toString -> value): _*)

  def isRecoverable(requestError: RequestError): Boolean = {
    Set[RequestError](
      ApplicationNameNotUnique
    ).contains(requestError)
  }

}

case class ErrorResponse(reason: RequestError, description: String)

object ErrorResponse {

  implicit val formatErrorResponse: Format[ErrorResponse] = Json.format[ErrorResponse]

}
