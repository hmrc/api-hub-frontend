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
import controllers.helpers.ErrorResultBuilder
import forms.ConfirmationFormProvider
import models.application.Application
import models.requests.BaseRequest
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import views.html.application.{DeleteApplicationConfirmationView, DeleteApplicationSuccessView}

import scala.concurrent.{ExecutionContext, Future}

class DeleteApplicationConfirmationController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  confirmView: DeleteApplicationConfirmationView,
  formProvider: ConfirmationFormProvider,
  apiHubService: ApiHubService,
  successView: DeleteApplicationSuccessView,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider("deleteApplicationConfirmation.error")

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id)) {
    implicit request =>
      Ok(confirmView(id, form, applicationSummaryList(request.application), request.identifierRequest.user))
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen applicationAuth(id)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(confirmView(id, formWithErrors, applicationSummaryList(request.application), request.identifierRequest.user))),
        _ =>
          apiHubService.deleteApplication(id, Some(request.identifierRequest.user.email)).map {
            case Some(_) => Ok(successView(request.identifierRequest.user))
            case None => applicationNotFound(id)
          }
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

  private def applicationNotFound(applicationId: String)(implicit request: BaseRequest[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.applicationNotFoundHeading"),
      message = Messages("site.applicationNotFoundMessage", applicationId)
    )
  }

}
