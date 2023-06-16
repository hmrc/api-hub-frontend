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

package controllers

import controllers.actions._
import forms.ProductionCredentialsChecklistFormProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ProductionCredentialsChecklistView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ProductionCredentialsChecklistController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  formProvider: ProductionCredentialsChecklistFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ProductionCredentialsChecklistView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(applicationId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)) {
    implicit request =>
      Ok(view(form, applicationId))
  }

  def onSubmit(applicationId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)) {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, applicationId)),
        _ =>
          Redirect(routes.GeneratePrimarySecretSuccessController.onPageLoad(applicationId))
      )
  }

}
