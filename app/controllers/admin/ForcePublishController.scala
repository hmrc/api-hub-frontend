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
import config.HipEnvironments
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.admin.ForcePublishPublisherReferenceFormProvider
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.ForcePublishViewModel
import views.html.admin.{ForcePublishSuccessView, ForcePublishView}

import scala.concurrent.ExecutionContext

@Singleton
class ForcePublishController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  apiHubService: ApiHubService,
  view: ForcePublishView,
  successView: ForcePublishSuccessView,
  formProvider: ForcePublishPublisherReferenceFormProvider,
  hipEnvironments: HipEnvironments,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request =>
      val viewModel = ForcePublishViewModel(form = form, user = request.user)
      Ok(view(viewModel))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          val viewModel = ForcePublishViewModel(form = formWithErrors, user = request.user)
          BadRequest(view(viewModel))
        },
        publisherReference =>
          Redirect(routes.ForcePublishController.showVersionComparison(publisherReference))
      )
  }

  def showVersionComparison(publisherReference: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      for {
        deploymentStatus <- apiHubService.getApiDeploymentStatus(hipEnvironments.deployTo, publisherReference)
        apiDetail <- apiHubService.getApiDetailForPublishReference(publisherReference)
        viewModel = ForcePublishViewModel(
          form = form.fill(publisherReference),
          user = request.user,
          publisherReference = Some(publisherReference),
          deploymentStatus = Some(deploymentStatus),
          catalogueVersion = apiDetail.map(_.version)
        )
      } yield Ok(view(viewModel))
  }

  def forcePublish(publisherReference: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      apiHubService.forcePublish(publisherReference).map {
        case Some(_) => Ok(successView(publisherReference, request.user))
        case None => errorResultBuilder.notFound(
          "forcePublish.apiNotFound.heading",
          Messages("forcePublish.apiNotFound.message", publisherReference)
        )
      }
  }

}
