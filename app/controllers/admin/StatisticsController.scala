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

package controllers.admin

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.admin.StatisticsView

import scala.concurrent.ExecutionContext

@Singleton
class StatisticsController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  view: StatisticsView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request =>
      Ok(view(request.user))
  }
  
  def apisInProduction(): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      apiHubService.apisInProduction().map { apis =>
        Ok(Json.toJson(apis))
      }
  }

  def listApisInProduction(): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      apiHubService.listApisInProduction().map { apiDetails =>
        Ok(Json.toJson(apiDetails.map(_.title).sortBy(_.toLowerCase)))
      }
  }

}
