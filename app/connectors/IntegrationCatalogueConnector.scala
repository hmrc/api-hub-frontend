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
import config.FrontendAppConfig
import models.api.{ApiDetail, IntegrationId, IntegrationResponse, PlatformContact}
import play.api.Logging
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.MimeTypes.JSON
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationCatalogueConnector @Inject()(
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig,
  frontEndConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) {

  import IntegrationCatalogueConnector._

  private val integrationCatalogueBaseUrl = servicesConfig.baseUrl("integration-catalogue")
  private val clientAuthToken = frontEndConfig.appAuthToken

  def getApiDetail(id: String)(implicit hc: HeaderCarrier): Future[Option[ApiDetail]] = {
    stringToIntegrationId(id) match {
      case Some(integrationId) =>
        httpClient.get(url"$integrationCatalogueBaseUrl/integration-catalogue/integrations/${integrationId.value.toString}")
          .setHeader((ACCEPT, JSON))
          .setHeader(AUTHORIZATION -> clientAuthToken)
          .execute[Either[UpstreamErrorResponse, ApiDetail]]
          .flatMap {
            case Right(apiDetail) => Future.successful(Some(apiDetail))
            case Left(e) if e.statusCode == NOT_FOUND => Future.successful(None)
            case Left(e) => Future.failed(e)
          }
      case _ => Future.successful(None)
    }
  }

  def getApis(platformFilter: Option[String])(implicit hc: HeaderCarrier): Future[Seq[ApiDetail]] = {
    val getApisUrl = s"$integrationCatalogueBaseUrl/integration-catalogue/integrations?integrationType=api" +
      platformFilter.fold("")(platform => s"&platformFilter=$platform")

    httpClient.get(url"$getApisUrl")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, IntegrationResponse]]
      .flatMap {
        case Right(integrationResponse) => Future.successful(integrationResponse.results)
        case Left(e) => Future.failed(e)
      }
  }

  def getPlatformContacts()(implicit hc: HeaderCarrier): Future[Seq[PlatformContact]] = {
    httpClient.get(url"$integrationCatalogueBaseUrl/integration-catalogue/platform/contacts")
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, Seq[PlatformContact]]]
      .flatMap {
        case Right(platformContacts) => Future.successful(platformContacts)
        case Left(e) => Future.failed(e)
      }
  }

  def filterApis(teamIds: Seq[String])(implicit hc: HeaderCarrier): Future[Seq[ApiDetail]] = {

    val queryParams = teamIds.map(id => ("teamIds", id))

    httpClient.get(url"$integrationCatalogueBaseUrl/integration-catalogue/integrations")
      .transform(wsRq => wsRq.withQueryStringParameters(queryParams: _*))
      .setHeader((ACCEPT, JSON))
      .setHeader(AUTHORIZATION -> clientAuthToken)
      .execute[Either[UpstreamErrorResponse, IntegrationResponse]]
      .flatMap {
        case Right(integrationResponse) => Future.successful(integrationResponse.results)
        case Left(e) => Future.failed(e)
      }
  }
}

object IntegrationCatalogueConnector extends Logging {

  def stringToIntegrationId(id: String): Option[IntegrationId] = {
    try {
      Some(IntegrationId(UUID.fromString(id)))
    }
    catch {
      case _: Throwable =>
        logger.debug(s"Invalid Integration Id specified: $id")
        None
    }
  }

}
