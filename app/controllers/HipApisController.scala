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

import com.google.inject.{Inject, Singleton}
import controllers.actions.OptionalIdentifierAction
import controllers.helpers.ErrorResultBuilder
import forms.SearchHipApisFormProvider
import models.api.ApiDetail
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HipApisView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipApisController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  view: HipApisView,
  errorResultBuilder: ErrorResultBuilder,
  optionallyIdentified: OptionalIdentifierAction,
  formProvider: SearchHipApisFormProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()
  def onPageLoad(): Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      apiHubService.getAllHipApis().map {
        case apiDetails: Seq[ApiDetail] => Ok(view(form, request.user, apiDetails))
        case _ => InternalServerError
      }
  }

  def onSubmit() : Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      Future.successful((NotImplemented))
  }

}
