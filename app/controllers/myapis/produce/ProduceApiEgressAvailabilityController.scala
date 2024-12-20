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

package controllers.myapis.produce

import controllers.actions.*
import models.{Mode, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEgressAvailabilityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiEgressAvailabilityController @Inject()(
                                                        override val messagesApi: MessagesApi,
                                                        identify: IdentifierAction,
                                                        val controllerComponents: MessagesControllerComponents,
                                                        view: ProduceApiEgressAvailabilityView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = identify {
    implicit request => Ok(view(request.user))
  }

  def onSubmit(answer: String): Action[AnyContent] = identify {
    implicit request =>
      answer match {
        case "yes" => Redirect(controllers.myapis.produce.routes.ProduceApiEgressSelectionController.onPageLoad())
        case "no" => Redirect(controllers.myapis.produce.routes.ProduceApiEgressPrefixesController.onPageLoad(NormalMode))
      }

  }

}
