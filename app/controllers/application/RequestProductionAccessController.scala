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
import controllers.helpers.ApplicationApiBuilder
import forms.{ApiPolicyConditionsDeclarationPageFormProvider, RequestProductionAccessDeclarationFormProvider}
import models.{ApiPolicyConditionsDeclaration, Mode}
import pages.RequestProductionAccessPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.RequestProductionAccessView
import views.html.defaultpages.notFound

import scala.concurrent.{ExecutionContext, Future}

class RequestProductionAccessController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: RequestProductionAccessView,
  applicationApiBuilder: ApplicationApiBuilder,
  formProvider: RequestProductionAccessDeclarationFormProvider)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(id: String, mode: Mode): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      applicationApiBuilder.build(request.application).map {
        case Right(applicationApis) => Ok(view(form, mode, request.application, applicationApis, Some(request.identifierRequest.user)))
        case Left(result) => result
      }

  }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      applicationApiBuilder.build(request.application).map {
        case Right(applicationApis) => form.bindFromRequest().fold(
          formWithErrors =>
            Ok(view(formWithErrors, mode, request.application, applicationApis, Some(request.identifierRequest.user))),
          value => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Left(result) => result
      }
  }
}
