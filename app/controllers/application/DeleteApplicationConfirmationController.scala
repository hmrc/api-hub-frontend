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
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import forms.ConfirmationFormProvider
import models.application.Application
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.application.DeleteApplicationConfirmationView

import scala.concurrent.ExecutionContext

class DeleteApplicationConfirmationController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: DeleteApplicationConfirmationView,
  formProvider: ConfirmationFormProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider("deleteApplicationConfirmation.error")

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id)) {
    implicit request =>
      Ok(view(id, form, applicationSummaryList(request.application)))
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen applicationAuth(id)) {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(id, formWithErrors, applicationSummaryList(request.application))),
        _ =>
          Redirect(controllers.application.routes.ApplicationDetailsController.onPageLoad(id))
      )
  }

  private def applicationSummaryList(application: Application)(implicit messages: Messages): SummaryList = {
    SummaryList(
      rows = Seq(
        SummaryListRowViewModel(
          key = "deleteApplicationConfirmation.applicationName",
          value = ValueViewModel(Text(application.name))
        )
      )
    )
  }

}
