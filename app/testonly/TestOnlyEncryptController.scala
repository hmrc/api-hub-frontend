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


import config.FrontendAppConfig
import controllers.actions._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, PlainText}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}



class TestOnlyEncryptController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           crypto: ApplicationCrypto,
                                           val controllerComponents: MessagesControllerComponents,
                                           servicesConfig: ServicesConfig,
                                           frontEndConfig: FrontendAppConfig,
                                           httpClient: HttpClientV2
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController{

private val applicationsBaseUrl = servicesConfig.baseUrl("api-hub-applications")
  private val clientAuthToken = frontEndConfig.appAuthToken

  def encrypt(email:String): Action[AnyContent] = Action.async {
    implicit request =>
      val emailEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(email)).value
      val emailEncoded = java.net.URLEncoder.encode(emailEncrypted, "UTF-8")
      val urlStr = f"$applicationsBaseUrl/api-hub-applications/test-only/decrypt/$emailEncoded"
      val url = url"$urlStr"
      val resp: Future[String] = httpClient
        .get(url)
        .setHeader((ACCEPT, JSON))
        .setHeader(AUTHORIZATION -> clientAuthToken)
        .execute[String]
      resp.map {
        case decryptedEmail => {
          assert(decryptedEmail == email)
          Ok(email)
        }
        case _ => Ok("FAILED")
      }
  }

}
