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
import config.HipEnvironments
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import models.application.{Environment, EnvironmentName}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.admin.EnvParityConfigTestView
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

@Singleton
class EnvParityConfigTestController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  apiHubService: ApiHubService,
  view: EnvParityConfigTestView,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request => Ok(view(request.user))
  }
  
  def fetchClientScopes(environmentName: EnvironmentName, clientId: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request => apiHubService.fetchClientScopes(hipEnvironments.forEnvironmentName(environmentName), clientId).map(_ match {
      case Some(scopes) => Ok(Json.toJson(scopes))
      case None => NotFound
    })
  }

  def fetchEgresses(environmentName: EnvironmentName): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request => apiHubService.fetchEgresses(hipEnvironments.forEnvironmentName(environmentName)).map(egresses => Ok(Json.toJson(egresses)))
  }

  def fetchDeployments(environmentName: EnvironmentName): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request => apiHubService.fetchDeployments(hipEnvironments.forEnvironmentName(environmentName)).map(deployments => Ok(Json.toJson(deployments)))
  }
}
