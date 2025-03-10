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

import com.google.inject.Inject
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import forms.ShutterChangeFormProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import services.HubStatusService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.ShutterChange
import views.html.ShutteredView
import views.html.admin.ShutterView

class ShutterController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  hubStatusService: HubStatusService,
  shutterView: ShutterView,
  formProvider: ShutterChangeFormProvider
) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def shutter(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request =>
      val filledForm = form.fill(ShutterChange(hubStatusService.isShuttered, Some(hubStatusService.shutterMessage)))
      Ok(shutterView(filledForm, request.user))
  }

  def changeShutter(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => BadRequest(shutterView(formWithErrors, request.user)),
        shutterChange => {
          if (shutterChange.shuttered) {
            hubStatusService.shutterDown(shutterChange.shutterMessage.getOrElse(hubStatusService.shutterMessage))
          }
          else {
            hubStatusService.shutterUp()
          }

          Redirect(controllers.admin.routes.ShutterController.shutter())
        }
      )
  }

}
