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

package controllers.apis

import com.google.inject.{Inject, Singleton}
import config.{Domains, Hods, Platforms}
import controllers.actions.OptionalIdentifierAction
import models.api.{ApiDetail, ApiDetailSummary}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.apis.ExploreApisView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExploreApisController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  view: ExploreApisView,
  optionallyIdentified: OptionalIdentifierAction,
  domains: Domains,
  hods: Hods,
  platforms: Platforms
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      apiHubService.getApis().map {
        case apiDetails: Seq[ApiDetailSummary] => Ok(view(request.user, apiDetails.sortWith( _.title.toUpperCase < _.title.toUpperCase), domains, hods, platforms))
        case null => InternalServerError
      }
  }

  def onSubmit() : Action[AnyContent] = optionallyIdentified.async {
    Future.successful(NotImplemented)
  }

}
