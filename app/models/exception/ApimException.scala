/*
 * Copyright 2025 HM Revenue & Customs
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

package models.exception

sealed trait ApimIssue

object ApimIssue {

  case object BadGateway extends ApimIssue
  case object NotFound extends ApimIssue
  case object UnexpectedResponse extends ApimIssue
  case object Error extends ApimIssue

}

case class ApimException(message: String, cause: Throwable, issue: ApimIssue) extends RuntimeException(message, cause)

object ApimException {

  def badGateway(): ApimException = {
    ApimException("Received bad Gateway calling APIM", null, ApimIssue.BadGateway)
  }

  def unexpectedResponse(status: Int): ApimException = {
    ApimException(s"Unexpected response $status returned calling APIM", null, ApimIssue.UnexpectedResponse)
  }

  def serviceNotFound(publisherRef: String): ApimException = {
    ApimException(s"Cannot find service $publisherRef", null, ApimIssue.NotFound)
  }

  def credentialNotFound(clientId: String): ApimException = {
    ApimException(s"Cannot find credential $clientId", null, ApimIssue.NotFound)
  }

  def error(cause: Throwable): ApimException = {
    ApimException(s"Error calling APIM", cause, ApimIssue.Error)
  }

}
