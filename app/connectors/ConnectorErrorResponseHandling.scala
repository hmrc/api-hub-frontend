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

package connectors

import models.errors.{ErrorResponse, RequestError}
import play.api.Logging
import play.api.libs.json.{JsError, Reads}
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

trait ConnectorErrorResponseHandling {
  this: Logging =>

  def responseBodyToModel[T : Reads](response: HttpResponse): Future[T] = {
    if (response.body.trim.isEmpty) {
      Future.failed(connectorException("No response body returned"))
    }
    else {
      response.json.validate[T].fold(
        errors => Future.failed(connectorException(s"Unable to deserialise response body: ${JsError.toJson(errors)}")),
        t => Future.successful(t)
      )
    }
  }

  def badRequest(response: HttpResponse): Future[RequestError] = {
    response.json.validate[ErrorResponse].fold(
      errors => {
        logger.debug(s"Unable to deserialise Bad Request response body: ${JsError.toJson(errors)}")
        Future.failed(connectorException(response))
      },
      errorResponse => {
        logger.info(errorResponse.toString)
        if (RequestError.isRecoverable(errorResponse.reason)) {
          Future.successful(errorResponse.reason)
        }
        else {
          Future.failed(connectorException(response))
        }
      }
    )
  }

  def connectorException(message: String): ConnectorException = {
    val exception = ConnectorException(message)
    logger.error(message)
    exception
  }

  def connectorException(response: HttpResponse): ConnectorException = {
    val exception = ConnectorException(response)
    logger.error(exception.message)
    exception
  }

}
