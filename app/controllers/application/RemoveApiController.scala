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

package controllers.application

import com.google.inject.{Inject, Singleton}
import config.HipEnvironments
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.YesNoFormProvider
import models.application.ApplicationLenses.*
import models.requests.ApplicationRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.{RemoveApiConfirmationView, RemoveApiSuccessView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveApiController @Inject()(
                                     val controllerComponents: MessagesControllerComponents,
                                     identify: IdentifierAction,
                                     applicationAuth: ApplicationAuthActionProvider,
                                     apiHubService: ApiHubService,
                                     errorResultBuilder: ErrorResultBuilder,
                                     formProvider: YesNoFormProvider,
                                     confirmationView: RemoveApiConfirmationView,
                                     successView: RemoveApiSuccessView,
                                     hipEnvironments: HipEnvironments,
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider("removeApiConfirmation.error")

  def onPageLoad(applicationId: String, apiId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)).async {
    implicit request =>
      showConfirmationView(Ok, apiId, form)
  }

  def onSubmit(applicationId: String, apiId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          showConfirmationView(BadRequest, apiId, formWithErrors),
        confirmed =>
          if (confirmed) {
            apiHubService.removeApi(applicationId, apiId).map {
              case Some(_) => Ok(successView(request.application, request.identifierRequest.user))
              case None => errorResultBuilder.apiNotFoundInApplication(apiId, request.application)
            }
          }
          else {
            Future.successful(Redirect(controllers.application.routes.ApplicationDetailsController.onPageLoad(applicationId)))
          }
      )
  }

  private def showConfirmationView(status: Status, apiId: String, form: Form[?])(implicit request: ApplicationRequest[?]): Future[Result] = {
    apiHubService.getApiDetail(apiId).map {
      case Some(apiDetail) if request.application.hasApi(apiDetail.id) =>
        status(confirmationView(request.application, apiDetail.id, apiDetail.title, form, request.identifierRequest.user))
      case Some(apiDetail) =>
        errorResultBuilder.apiNotFoundInApplication(apiDetail, request.application)
      case None =>
        request.application.apis.find(_.id.equals(apiId))
          .map(
            api =>
              status(confirmationView(request.application, api.id, api.title, form, request.identifierRequest.user))
          )
          .getOrElse(errorResultBuilder.apiNotFound(apiId))
    }
  }

}
