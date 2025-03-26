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

package controllers

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import controllers.actions.OptionalIdentifierAction
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.ServiceStartViewModel
import views.html.ServiceStartView

import scala.concurrent.ExecutionContext

@Singleton
class ServiceStartController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  optionalIdentifierAction: OptionalIdentifierAction,
  frontendAppConfig: FrontendAppConfig,
  apiHubService: ApiHubService,
  view: ServiceStartView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = optionalIdentifierAction.async {
    implicit request =>
      apiHubService.fetchDashboardStatistics().map(
        dashboardStatistics =>
          Ok(view(
            ServiceStartViewModel(
              user = request.user,
              dashboardStatistics = dashboardStatistics,
              frontendAppConfig.startPageLinks
            )
          ))
      )
  }

}
