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

package controllers.deployment

import com.google.inject.{Inject, Singleton}
import connectors.ApplicationsConnector
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import forms.mappings.Mappings
import models.deployment.{InvalidOasResponse, RedeploymentRequest, SuccessfulDeploymentsResponse}
import models.requests.ApiRequest
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.deployment.{DeploymentFailureView, DeploymentSuccessView, SimpleApiRedeploymentView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleApiRedeploymentController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  apiAuth: ApiAuthActionProvider,
  applicationsConnector: ApplicationsConnector,
  view: SimpleApiRedeploymentView,
  successView: DeploymentSuccessView,
  failureView: DeploymentFailureView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import SimpleApiRedeploymentController._

  private val form = new RedeploymentRequestFormProvider()()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen apiAuth(id)) {
    implicit request =>
      showView(OK, form)
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen apiAuth(id)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(showView(BAD_REQUEST, formWithErrors)),
        redeploymentRequest => applicationsConnector.updateDeployment(request.apiDetails.publisherReference, redeploymentRequest).map {
          case Some(response: SuccessfulDeploymentsResponse) =>
            Ok(successView(request.identifierRequest.user, response))
          case Some(response: InvalidOasResponse) =>
            BadRequest(failureView(request.identifierRequest.user, response.failure, controllers.deployment.routes.SimpleApiRedeploymentController.onPageLoad(id).url))
          case None =>
            NotFound
        }
      )
  }

  private def showView(code: Int, form: Form[_])(implicit request: ApiRequest[_]): Result = {
    Status(code)(view(form, request.apiDetails, request.identifierRequest.user))
  }

}

object SimpleApiRedeploymentController {

  class RedeploymentRequestFormProvider extends Mappings {

    def apply(): Form[RedeploymentRequest] =
      Form(
        mapping(
          "description" -> text("Enter a description"),
          "oas" -> text("Enter the OAS"),
          "status" -> text("Enter an API status")
        )(RedeploymentRequest.apply)(RedeploymentRequest.unapply)
      )

  }

}
