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

package testonly

import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyInternalAuthConnector @Inject()(
  httpClientV2  : HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) {

  import TestOnlyInternalAuthConnector._

  private val internalAuthBaseUrl = servicesConfig.baseUrl("internal-auth")

  def testOnlyCreateToken(data: TestOnlyAddTokenRequest)(implicit hc: HeaderCarrier): Future[Token] = {
    implicit val tr: Reads[Token] = tokenReads
    implicit val bodyFormat: OFormat[TestOnlyAddTokenRequest] = TestOnlyAddTokenRequest.format
    httpClientV2
      .post(url"$internalAuthBaseUrl/test-only/token")
      .withBody(Json.toJson(data))
      .execute[Token]
  }

  def testOnlyGetTokenData(token: Token)(implicit hc: HeaderCarrier): Future[Option[TokenData]] = {
    implicit val responseFormat: OFormat[TokenData] = TokenData.format
    httpClientV2
      .get(url"$internalAuthBaseUrl/test-only/token")
      .setHeader("Authorization" -> token.value)
      .execute[Option[TokenData]]
  }

  def testOnlyDeleteToken(token: Token)(implicit hc: HeaderCarrier): Future[Unit] = {
    val httpReads = HttpReadsInstances.throwOnFailure(HttpReadsInstances.readEitherOf[Unit])
    httpClientV2
      .post(url"$internalAuthBaseUrl/internal-auth/token/revoke")
      .setHeader("Authorization" -> token.value)
      .execute[Unit](httpReads, implicitly[ExecutionContext])
  }
}

object TestOnlyInternalAuthConnector {
  case class Username(value: String) extends AnyVal

  case class Password(value: String) extends AnyVal

  case class Token(value: String) extends AnyVal

  val tokenReads: Reads[Token] =
    (__ \ "token").read[String].map(Token.apply)

  sealed trait AuthError
  object AuthError {
    case object LoginFailed extends AuthError
  }
}
