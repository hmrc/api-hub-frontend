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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import controllers.actions.IdentifierAction
import controllers.helpers.{ErrorResultBuilder, Fetching}
import forms.admin.ApprovalDecisionFormProvider
import models.accessrequest.{AccessRequest, Pending}
import models.application.Application
import models.application.ApplicationLenses.*
import models.requests.IdentifierRequest
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.AccessRequestViewModel
import views.html.admin.AccessRequestView

import scala.concurrent.ExecutionContext

@Singleton
class AccessRequestController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  override val apiHubService: ApiHubService,
  override val errorResultBuilder: ErrorResultBuilder,
  formProvider: ApprovalDecisionFormProvider,
  view: AccessRequestView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Fetching {

  private val form = formProvider()

  def onPageLoad(accessRequestId: String): Action[AnyContent] = identify.async {
    implicit request =>
      (for {
        accessRequest <- EitherT(fetchAccessRequestOrNotFound(accessRequestId))
        application <- EitherT(fetchApplicationOrNotFound(accessRequest.applicationId))
        result <- EitherT.cond(
          application.isAccessible(request.user),
          buildView(accessRequest, application),
          Redirect(controllers.routes.UnauthorisedController.onPageLoad)
        )
      } yield result).merge
  }

  private def buildView(accessRequest: AccessRequest, application: Application)(implicit request: IdentifierRequest[?]): Result = {
      val model = AccessRequestViewModel.consumerViewModel(
          application,
          accessRequest,
          request.user
        )
      Ok(view(model, form, request.user, accessRequest.status == Pending))
  }
}
