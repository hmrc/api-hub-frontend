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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.StringContextOps
import viewmodels.RelatedContentLink

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "api-hub-frontend"

  val supportEmailAddress = configuration.get[String]("support.emailAddress")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${url"$host${request.uri}"}"

  val loginWithLdapUrl: String   = configuration.get[String]("urls.loginWithLdap")
  val loginWithStrideUrl: String = configuration.get[String]("urls.loginWithStride")
  val loginContinueUrl: String   = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String         = configuration.get[String]("urls.signOut")
  val appAuthToken: String       = configuration.get[String]("internal-auth.token")

  val helpDocsPath: String       = configuration.get[String]("api-hub-guide.service-path")
  
  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/api-hub-frontend"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val shuttered: Boolean = configuration.get[Boolean]("hubStatus.shuttered")
  val shutterMessage: String = configuration.get[String]("hubStatus.shutterMessage")

  val feedbackLink: String = configuration.get[String]("feedback-link")
  val wireMockGuidanceLink: String = configuration.get[String]("wiremock-guidance-link")

  val maxOasUploadSizeMb: Int = configuration.get[Int]("maxOasUploadSizeMb")
  val oasUploadValidExtensions: Set[String] = configuration.get[Seq[String]]("oasUploadValidExtensions").toSet
  val maxWiremockUploadSizeMb: Int = configuration.get[Int]("maxWiremockUploadSizeMb")
  val wiremockUploadValidExtensions: Set[String] = configuration.get[Seq[String]]("wiremockUploadValidExtensions").toSet

  val dashboardApplicationsToShow: Int = configuration.get[Int]("dashboardApplicationsToShow")
  val dashboardTeamsToShow: Int = configuration.get[Int]("dashboardTeamsToShow")
  val dashboardApisToShow: Int = configuration.get[Int]("dashboardApisToShow")

  val showApisOnDashboard: Boolean = configuration.getOptional[Boolean]("features.showApisOnDashboard").getOrElse(false)

  val hipEnvironmentsLookupTimeoutSeconds: Int = configuration.get[Int]("hipEnvironmentsLookupTimeoutSeconds")
  val hubStatusTimeoutSeconds: Int = configuration.get[Int]("hubStatusTimeoutSeconds")

  val startPageLinks: Seq[RelatedContentLink] = configuration.get[Seq[RelatedContentLink]]("startPage.links")

}
