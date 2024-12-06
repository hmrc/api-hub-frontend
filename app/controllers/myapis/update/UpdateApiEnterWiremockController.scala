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

package controllers.myapis.update

import controllers.actions.*
import models.Mode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEnterWiremockView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UpdateApiEnterWiremockController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ProduceApiEnterWiremockView,
                                       getData: ProduceApiDataRetrievalAction,
                                       requireData: DataRequiredAction,
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = identify {
    implicit request => Ok(view(controllers.myapis.update.routes.UpdateApiEnterWiremockController.onSubmit(mode)))
  }

  def onPageLoadWithUploadedWiremock(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => Ok(view(controllers.myapis.produce.routes.ProduceApiEnterWiremockController.onSubmit(mode)))
  }
  
  def onSubmit(mode: Mode): Action[AnyContent] = identify {
    implicit request => Redirect(controllers.myapis.update.routes.UpdateApiAddPrefixesController.onPageLoad(mode))
  }

}
