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

package controllers

import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DashboardViewModel
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 identify: IdentifierAction,
                                 crypto: ApplicationCrypto,
                                 view: IndexView,
                                 apiHubService: ApiHubService,
                                 frontendAppConfig: FrontendAppConfig
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    val maxApplicationsToShow = 5
    val maxTeamsToShow = 5

    val encryptedEmail = crypto.QueryParameterCrypto.encrypt(PlainText(request.user.email)).value
    implicit val hc2: HeaderCarrier = hc.withExtraHeaders(("Encrypted-User-Email", encryptedEmail))
    for {
      userApps <- apiHubService.getApplications(Some(request.user.email), false)
      userTeams <- apiHubService.findTeams(Some(request.user.email))
      userApis <- apiHubService.getUserApis(request.user)
      sortedUserApis = userApis.sortWith((a, b) => a.created.isAfter(b.created))
    } yield Ok(view(
          DashboardViewModel(frontendAppConfig, userApps, userTeams, sortedUserApis, request.user)
    ))
  }

}
