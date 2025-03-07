/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.admin.FeatureStatusChangeFormProvider
import models.hubstatus.FrontendShutter
import models.user.UserModel
import play.api.data.{Form, FormError}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.HubStatusService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.FeatureStatusChange
import views.html.admin.{ShutterSuccessView, ShutterView}

import scala.concurrent.ExecutionContext

@Singleton
class ShutterController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  hubStatusService: HubStatusService,
  formProvider: FeatureStatusChangeFormProvider,
  shutterView: ShutterView,
  shutterSuccessView: ShutterSuccessView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      hubStatusService.status(FrontendShutter).map(
        featureStatus =>
          Ok(shutterView(form, featureStatus, request.user))
      )
  }

  def onSubmit(): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      val boundForm = form.bindFromRequest()
      boundForm.fold(
        formWithErrors => badRequest(formWithErrors, request.user),
        {
          case FeatureStatusChange(shuttered, None) if !shuttered =>
            hubStatusService.shutterUp(FrontendShutter).map(
              featureStatus => Ok(shutterSuccessView(featureStatus, request.user))
            )
          case FeatureStatusChange(shuttered, Some(shutterMessage)) if shuttered =>
            hubStatusService.shutterDown(FrontendShutter, shutterMessage).map(
              featureStatus => Ok(shutterSuccessView(featureStatus, request.user))
            )
          case _ =>
            badRequest(
              boundForm.withError(FormError("shutterMessage", "shutter.shutterMessage.error.required")),
              request.user
            )
        }
      )
  }

  private def badRequest(formWithErrors: Form[?], user: UserModel)(implicit request: Request[?]) = {
    hubStatusService.status(FrontendShutter).map(
      featureStatus =>
        BadRequest(shutterView(formWithErrors, featureStatus, user))
    )
  }

}
