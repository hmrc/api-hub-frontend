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

import controllers.actions.OptionalIdentifierAction
import forms.FeedbackFormProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.FeedbackView

import javax.inject.Inject

class FeedbackController @Inject()(
                                    val controllerComponents: MessagesControllerComponents,
                                    optionallyIdentified: OptionalIdentifierAction,
                                    formProvider: FeedbackFormProvider,
                                    view: FeedbackView
                                  ) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = optionallyIdentified { implicit request =>
    Ok(view(form, request.user))
  }

  def onSubmit: Action[AnyContent] = optionallyIdentified { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        BadRequest(view(formWithErrors, request.user)),
      value =>
        //TODO: this should send an email to HIPP on future tickets
        Redirect(controllers.routes.ServiceStartController.onPageLoad)
    )
  }
}
