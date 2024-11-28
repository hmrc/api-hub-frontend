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

package controllers.application

import com.google.inject.Inject
import config.{FrontendAppConfig, HipEnvironments}
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ApplicationApiBuilder
import models.application.ApplicationLenses.*
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.ApplicationDetailsView

import scala.concurrent.ExecutionContext

class ApplicationDetailsController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: ApplicationDetailsView,
  applicationApiBuilder: ApplicationApiBuilder,
)(implicit ec: ExecutionContext, config: FrontendAppConfig, hipEnvironments: HipEnvironments) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      applicationApiBuilder.build(request.application).map(
        applicationApis =>
          Ok(view(
            request.application.withSortedTeam(),
            applicationApis,
            Some(request.identifierRequest.user)
          ))
      )
  }

}
