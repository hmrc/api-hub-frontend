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

package controllers.myapis.promote

import com.google.inject.{Inject, Singleton}
import config.{FrontendAppConfig, HipEnvironments}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.myapis.promote.MyApiSetEgressForm
import models.api.ApiDetail
import models.requests.ApiRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.promote.MyApiSetEgressViewModel
import views.html.myapis.promote.MyApiSetEgressView
import services.ApiHubService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyApiSetEgressController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  view: MyApiSetEgressView,
  identify: IdentifierAction,
  config: FrontendAppConfig,
  apiAuth: ApiAuthActionProvider,
  errorResultBuilder: ErrorResultBuilder,
  hipEnvironments: HipEnvironments,
  apiHubService: ApiHubService,
  formProvider: MyApiSetEgressForm
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  private val form = formProvider()

  def onPageLoad(id: String, environment: String): Action[AnyContent] = (identify andThen apiAuth(id)).async  {
    implicit request =>
      buildView(form, request.apiDetails, environment)
  }

  def onSubmit(id: String, environment: String): Action[AnyContent] = (identify andThen apiAuth(id)).async {
    implicit request => {
      form.bindFromRequest().fold(
        formWithErrors => buildView(formWithErrors, request.apiDetails, environment),
        egress =>
          Future.successful(Redirect(controllers.myapis.promote.routes.MyApiPromoteSuccessController.onPageLoad(id, environment)))
      )
    }
  }

  private def buildView(form: Form[?], apiDetail: ApiDetail, environment: String)(implicit request: ApiRequest[AnyContent]) = {
    (for {
      fromEnvironment <- hipEnvironments.forEnvironmentIdOptional(environment)
      toEnvironment <- hipEnvironments.promotionEnvironment(fromEnvironment)
    } yield for {
      egresses <- apiHubService.listEgressGateways(toEnvironment)
      deploymentStatuses <- apiHubService.getApiDeploymentStatuses(apiDetail.publisherReference)
      viewModel = MyApiSetEgressViewModel(apiDetail, fromEnvironment, toEnvironment, request.maybeUser, egresses, deploymentStatuses)
    } yield Ok(view(form, viewModel))).getOrElse(
      Future.successful(errorResultBuilder.environmentNotFound(environment))
    )
  }

}
