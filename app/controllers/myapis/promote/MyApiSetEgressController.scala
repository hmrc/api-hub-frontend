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

package controllers.myapis.promote

import com.google.inject.{Inject, Singleton}
import config.{FrontendAppConfig, HipEnvironments}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.promote.MyApiSetEgressView
import scala.concurrent.ExecutionContext

@Singleton
class MyApiSetEgressController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  view: MyApiSetEgressView,
  identify: IdentifierAction,
  config: FrontendAppConfig,
  apiAuth: ApiAuthActionProvider,
  errorResultBuilder: ErrorResultBuilder,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String, environment: String): Action[AnyContent] = (identify andThen apiAuth(id))  {
    implicit request =>
      (for {
        fromEnvironment <- hipEnvironments.forEnvironmentIdOptional(environment)
        toEnvironment <- hipEnvironments.promotionEnvironment(fromEnvironment)
      } yield Ok(view(request.apiDetails, fromEnvironment, toEnvironment, request.identifierRequest.user))).getOrElse(
        errorResultBuilder.environmentNotFound(environment)
      )
  }

  def onSubmit(id: String, environment: String): Action[AnyContent] = (identify andThen apiAuth(id)) {
    implicit request =>
      Redirect(controllers.myapis.promote.routes.MyApiPromoteSuccessController.onPageLoad(id, environment))
  }

}
