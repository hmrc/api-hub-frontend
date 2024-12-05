/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import com.google.inject.Singleton
import models.api.ApiDetail
import models.application.{Application, Secondary}
import models.application.ApplicationLenses.*
import models.curl.OpenApiDoc
import models.{ApiWorld, CurlCommand}
import play.api.Logging
import play.api.i18n.MessagesProvider

import java.util.Base64.getEncoder

@Singleton
class CurlCommandService extends Logging {

  def buildCurlCommandsForApi(application: Application, apiDetail: ApiDetail, apiWorld: ApiWorld)
                             (implicit messagesProvider: MessagesProvider): Either[String, Seq[CurlCommand]] = {
    OpenApiDoc.parse(apiDetail.openApiSpecification).map(openApiDoc =>
      val operationResults = for {
        api <- application.apis
        selectedEndpoint <- api.endpoints
      } yield openApiDoc.getOperation(selectedEndpoint.httpMethod, selectedEndpoint.path)

      operationResults.collect({ case Left(errorMessage) => logger.warn(errorMessage) })

      operationResults.collect({ case Right(operation) => operation }).map(operation =>
        CurlCommand(
          method = operation.method,
          server = openApiDoc.getServerForApiWorld(apiWorld),
          path = operation.path,
          queryParams = operation.queryParams,
          pathParams = operation.pathParams,
          headers = operation.headerParams ++ getCommonHeaders(application),
          requestBody = operation.exampleRequestBody
        )
      )
    )
  }

  private def getCommonHeaders(application: Application): Map[String,String] = {
    val maybeAuthHeader = for {
      credential <- application.getMasterCredential(Secondary)
      secret <- credential.clientSecret
      credentials = s"${credential.clientId}:$secret"
      encodedCredentials = getEncoder.encodeToString(credentials.getBytes)
    } yield s"Basic $encodedCredentials"

    Map(
      "Content-Type" -> Some("application/json"),
      "Authorization" -> maybeAuthHeader
    ).collect({ case (key, Some(value)) => key -> value })
  }
}
