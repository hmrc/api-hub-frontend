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

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

class ShutterController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig
) extends FrontendBaseController with SimpleRouter with I18nSupport {

  override def routes: Routes =  {
    case _ => this.shuttered()
  }

  def shuttered(): Action[AnyContent] = Action {
    Ok(config.shutterMessage)
  }

}