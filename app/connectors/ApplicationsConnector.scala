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

import com.google.inject.{Inject, Singleton}
import connectors.ApplicationsConnector.{RequestBuilderExtensionOps, deOptionalise}
import models.application.{Application, NewApplication, NewScope}
import models.errors.{ErrorResponse, RequestError}
import play.api.Logging
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.http.MimeTypes.JSON
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpReads, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsConnector @Inject()(
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext) extends Logging {

  private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")

  def registerApplication(newApplication: NewApplication)(implicit hc: HeaderCarrier): Future[Either[RequestError, Application]] = {
    postWithRequiredResponse[NewApplication, Application](
      url"$applicationsBaseUrl/api-hub-applications/applications",
      Some(newApplication),
      Seq((ACCEPT, JSON))
    )
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications")
      .setHeader((ACCEPT, JSON))
      .execute[Seq[Application]]
  }

  def getApplication(id:String)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/$id")
      .setHeader((ACCEPT, JSON))
      .execute[Either[UpstreamErrorResponse, Application]]
      .flatMap {
        case Right(application) => Future.successful(Some(application))
        case Left(e) if e.statusCode==404 => Future.successful(None)
        case Left(e) => Future.failed(e)
      }
  }

  def requestAdditionalScope(id: String, newScope: NewScope)(implicit hc: HeaderCarrier): Future[Either[RequestError, Option[Unit]]] = {
    postWithOptionalResponse[Seq[NewScope], Unit](
      url"$applicationsBaseUrl/api-hub-applications/applications/$id/environments/scopes",
      Some(Seq(newScope)),
      Seq.empty
    )
  }

  def pendingScopes()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    httpClient
      .get(url"$applicationsBaseUrl/api-hub-applications/applications/pending-scopes")
      .setHeader((ACCEPT, JSON))
      .execute[Seq[Application]]
  }

  def approveProductionScope(appId: String, scopeName: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    httpClient
      .put(url"$applicationsBaseUrl/api-hub-applications/applications/$appId/environments/prod/scopes/$scopeName")
      .setHeader((CONTENT_TYPE, JSON))
      .withBody("{\"status\":\"APPROVED\"}")
      .execute[Either[UpstreamErrorResponse, Unit]]
      .flatMap {
        case Right(_) => Future.successful(true)
        case Left(e) if e.statusCode == 404 => Future.successful(false)
        case Left(e) => Future.failed(e)
      }
  }
  private def postWithOptionalResponse[S: Writes, T: HttpReads]
    (url: URL, body: Option[S] = None, headers: Seq[(String, String)] = Seq.empty)
    (implicit hc: HeaderCarrier): Future[Either[RequestError, Option[T]]] = {

    val method = "POST"

    httpClient
      .post(url)
      .withOptionalBody(body)
      .setHeader(headers: _*)
      .executeX[T](method, url)
  }

  private def postWithRequiredResponse[S: Writes, T: HttpReads]
    (url: URL, body: Option[S] = None, headers: Seq[(String, String)] = Seq.empty)
    (implicit hc: HeaderCarrier): Future[Either[RequestError, T]] = {

    val method = "POST"

    deOptionalise(
      method,
      url,
      postWithOptionalResponse[S, T](url, body, headers)
    )
  }

}

object ApplicationsConnector extends Logging {

  implicit class RequestBuilderExtensionOps(requestBuilder: RequestBuilder) {

    def withOptionalBody[B : Writes](body: Option[B])(implicit ec: ExecutionContext): RequestBuilder = {
      body match {
        case Some(b) => requestBuilder.withBody(Json.toJson(b))
        case _ => requestBuilder
      }
    }

    def executeX[T : HttpReads](method: String, url: URL)(implicit ec: ExecutionContext): Future[Either[RequestError, Option[T]]] = {
      requestBuilder.execute[HttpResponse]
        .flatMap(
          response =>
            response.status match {
              case BAD_REQUEST => processBadRequest(method, url, response)
              case NOT_FOUND => processNotFound(method, url)
              case _ => processResultOrError[T](method, url, response)
            }
        )
    }
  }

  private def processBadRequest[T](method: String, url: URL, response: HttpResponse): Future[Either[RequestError, Option[T]]] = {
    response.json.validate[ErrorResponse].fold(
      _ => {
        logger.warn(s"Received unexpected Bad Request response for $method call to ${url.toString}")
        Future.failed(
          UpstreamErrorResponse(
            HttpErrorFunctions.upstreamResponseMessage(method, url.toString, response.status, response.body),
            response.status
          )
        )
      },
      requestError => {
        logger.info(s"Received expected Bad Request call $requestError for $method call to ${url.toString}")
        Future.successful(Left(requestError.reason))
      }
    )
  }

  private def processNotFound[T](method: String, url: URL): Future[Either[RequestError, Option[T]]] = {
    logger.warn(s"Received Not Found response for $method call to ${url.toString}")
    Future.successful(Right(None))
  }

  private def processResultOrError[T: HttpReads](method: String, url: URL, response: HttpResponse): Future[Either[RequestError, Option[T]]] = {
    HttpReadsInstances.readEitherOf[T]
      .read(method, url.toString, response).fold(
      upstreamErrorResponse => {
        logger.error(s"Received upstream error for $method call to ${url.toString}", upstreamErrorResponse)
        Future.failed(upstreamErrorResponse)
      },
      t => {
        logger.debug(s"Received response body of type ${t.getClass.getSimpleName} for $method call to ${url.toString}")
        Future.successful(Right(Some(t)))
      }
    )
  }

  private def deOptionalise[T](method: String, url: URL, result: Future[Either[RequestError, Option[T]]])
      (implicit ec: ExecutionContext): Future[Either[RequestError, T]] = {
    result.flatMap {
      case Right(Some(t)) => Future.successful(Right(t))
      case Right(None) =>
        Future.failed(
          UpstreamErrorResponse(
            HttpErrorFunctions.upstreamResponseMessage(method, url.toString, NOT_FOUND, ""),
            NOT_FOUND
          )
        )
      case Left(error) => Future.successful(Left(error))
    }
  }

}
