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

package controllers.myapis

import com.google.inject.{Inject, Singleton}
import config.{Domains, Hods}
import connectors.ApplicationsConnector
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.myapis.SimpleApiDeploymentController.{transformFromEgressMappings, transformFromPrefixesToRemove, transformToEgressMappings, transformToPrefixesToRemove}
import forms.mappings.Mappings
import models.deployment.{EgressMapping, InvalidOasResponse, RedeploymentRequest, SuccessfulDeploymentsResponse}
import models.requests.ApiRequest
import play.api.data.{Form, Forms}
import play.api.data.Forms.{mapping, optional}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiRedeploymentView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleApiRedeploymentController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  apiAuth: ApiAuthActionProvider,
  apiHubService: ApiHubService,
  applicationsConnector: ApplicationsConnector,
  view: SimpleApiRedeploymentView,
  successView: DeploymentSuccessView,
  failureView: DeploymentFailureView,
  domains: Domains,
  hods: Hods
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import SimpleApiRedeploymentController._

  private val form = new RedeploymentRequestFormProvider()()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen apiAuth(id)).async {
    implicit request =>
      apiHubService.getDeploymentDetails(request.apiDetails.publisherReference).map {
        case Some(deploymentDetails) =>
          val filledForm = form.fill(
            RedeploymentRequest(
              description = deploymentDetails.description,
              oas = "", // We don't get this back with DeploymentDetails
              status = deploymentDetails.status,
              domain = deploymentDetails.domain,
              subDomain = deploymentDetails.subDomain,
              hods = deploymentDetails.hods,
              prefixesToRemove = deploymentDetails.prefixesToRemove,
              egressMappings = deploymentDetails.egressMappings
            )
          )

          showView(OK, filledForm)
        case None =>
          showView(OK, form)
      }
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen apiAuth(id)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(showView(BAD_REQUEST, formWithErrors)),
        redeploymentRequest => applicationsConnector.updateDeployment(request.apiDetails.publisherReference, redeploymentRequest).map {
          case Some(response: SuccessfulDeploymentsResponse) =>
            Ok(successView(request.identifierRequest.user, response))
          case Some(response: InvalidOasResponse) =>
            BadRequest(failureView(request.identifierRequest.user, response.failure, controllers.myapis.routes.SimpleApiRedeploymentController.onPageLoad(id).url))
          case None =>
            NotFound
        }
      )
  }

  private def showView(code: Int, form: Form[?])(implicit request: ApiRequest[?]): Result = {
    Status(code)(view(form, request.apiDetails, domains, hods, request.identifierRequest.user))
  }

}

object SimpleApiRedeploymentController {

  class RedeploymentRequestFormProvider extends Mappings {

    def apply(): Form[RedeploymentRequest] =
      Form(
        mapping(
          "description" -> text("Enter a description"),
          "oas" -> text("Enter the OAS"),
          "status" -> text("Enter an API status"),
          "domain" -> text("Enter a domain"),
          "subdomain" -> text("Enter a subdomain"),
          "hods" -> Forms.seq(text()),
          "prefixesToRemove" -> optional(text()).transform[Seq[String]](transformToPrefixesToRemove, transformFromPrefixesToRemove),
          "egressMappings" -> optional(text()).transform[Option[Seq[EgressMapping]]](transformToEgressMappings, transformFromEgressMappings)
        )(RedeploymentRequest.apply)(o => Some(Tuple.fromProductTyped(o)))
      )

  }

}
