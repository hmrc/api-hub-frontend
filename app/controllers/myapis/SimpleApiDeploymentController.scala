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
import controllers.actions.IdentifierAction
import forms.mappings.Mappings
import models.deployment.{DeploymentsRequest, EgressMapping, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.data.Forms.{mapping, optional}
import play.api.data.*
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.DeploymentSuccessViewModel
import views.html.myapis.{DeploymentFailureView, DeploymentSuccessView, SimpleApiDeploymentView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleApiDeploymentController @Inject()(
                                               val controllerComponents: MessagesControllerComponents,
                                               identify: IdentifierAction,
                                               deploymentView: SimpleApiDeploymentView,
                                               apiHubService: ApiHubService,
                                               applicationsConnector: ApplicationsConnector,
                                               deploymentSuccessView: DeploymentSuccessView,
                                               deploymentFailureView: DeploymentFailureView,
                                               domains: Domains,
                                               hods: Hods
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  import SimpleApiDeploymentController._

  private val form = new DeploymentsRequestFormProvider()()

  def onPageLoad(): Action[AnyContent] = identify.async {
    implicit request =>
      showView(OK, form)
  }

  def onSubmit(): Action[AnyContent] = identify.async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => showView(BAD_REQUEST, formWithErrors),
        deploymentsRequest =>
          applicationsConnector
            .generateDeployment(deploymentsRequest)
            .map {
              case response: SuccessfulDeploymentsResponse =>
                logger.info(s"Successful deployments response${System.lineSeparator()}${Json.prettyPrint(Json.toJson(response))}")
                Ok(deploymentSuccessView(DeploymentSuccessViewModel(
                  request.user,
                  response.id,
                  deploymentsRequest.name,
                  "api.deployment.success.feedback.create.heading",
                  "api.deployment.success.feedback.create.message"
                )))
              case response: InvalidOasResponse =>
                logger.info(s"Invalid OAS deployments response${System.lineSeparator()}${Json.prettyPrint(Json.toJson(response))}")
                BadRequest(deploymentFailureView(request.user, response.failure, controllers.myapis.routes.SimpleApiDeploymentController.onPageLoad().url))
            }
      )
  }

  private def showView(code: Int, form: Form[?])(implicit request: IdentifierRequest[?]): Future[Result] = {
    apiHubService.findTeams(Some(request.user.email))
      .map(teams => teams.sortBy(_.name.toLowerCase))
      .map(teams => Status(code)(deploymentView(form, teams, domains, hods, request.user)))
  }

}

object SimpleApiDeploymentController {

  class DeploymentsRequestFormProvider() extends Mappings {

    def apply(): Form[DeploymentsRequest] =
      Form(
        mapping(
        "lineOfBusiness" -> text("Enter a line of business"),
        "name" -> text("Enter a name"),
        "description" -> text("Enter a description"),
        "egress" -> optional(text("Enter an egress")),
        "teamId" -> text("Select a team"),
        "oas" -> text("Enter the OAS"),
        "passthrough" -> Forms.default(boolean(), false),
        "status" -> text("Enter an API status"),
        "domain" -> text("Enter a domain"),
        "subdomain" -> text("Enter a subdomain"),
        "hods" -> Forms.seq(text()),
        "prefixesToRemove" -> optional(text())
          .transform[Seq[String]](transformToPrefixesToRemove, transformFromPrefixesToRemove),
        "egressMappings" -> optional(text())
          .verifying("Each Egress Prefix Mapping must contain exactly one comma", optionalTextToSeq andThen allContainExactlyOneComma)
          .transform[Option[Seq[EgressMapping]]](transformToEgressMappings, transformFromEgressMappings)
        )(DeploymentsRequest.apply)(o => Some(Tuple.fromProductTyped(o)))
      )

  }

  def transformToPrefixesToRemove(text: Option[String]): Seq[String] = {
    optionalTextToSeq(text)
  }

  def transformFromPrefixesToRemove(prefixes: Seq[String]): Option[String] = {
    if (prefixes.nonEmpty) {
      Some(prefixes.mkString(System.lineSeparator()))
    }
    else {
      None
    }
  }

  def transformToEgressMappings(text: Option[String]): Option[Seq[EgressMapping]] = {
    optionalTextToSeq(text) match {
      case Nil => None
      case mappings => Some(mappings.map { mapping =>
        val Array(prefix, egressPrefix) = mapping.split(",", 2).map(_.trim)
        EgressMapping(prefix, egressPrefix)
      })
    }
  }

  def transformFromEgressMappings(mappings: Option[Seq[EgressMapping]]): Option[String] = {
    mappings.map(_.map(mapping => s"${mapping.prefix},${mapping.egressPrefix}").mkString(System.lineSeparator()))
  }
  
  def optionalTextToSeq(text: Option[String]): Seq[String] = {
    Seq.from(text.getOrElse("").split("""\R""")).map(_.trim).filter(_.nonEmpty)
  }
  
  def allContainExactlyOneComma(strings: Seq[String]): Boolean = {
    strings.forall(_.count(_ == ',') == 1)
  }

}
