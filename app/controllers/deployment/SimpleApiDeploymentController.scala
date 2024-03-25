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
import controllers.actions.IdentifierAction
import models.deployment.{DeploymentsRequest, InvalidOasResponse, SuccessfulDeploymentsResponse}
import play.api.Logging
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.deployment.SimpleApiDeploymentView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleApiDeploymentController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  view: SimpleApiDeploymentView,
  applicationsConnector: ApplicationsConnector
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  import SimpleApiDeploymentController._

  def onPageLoad(): Action[AnyContent] = identify {
    implicit request =>
      Ok(view(deploymentsRequestForm, request.user))
  }

  def onSubmit(): Action[AnyContent] = identify.async {
    implicit request =>
      deploymentsRequestForm.bindFromRequest().fold(
        _ => Future.successful(BadRequest),
        deploymentsRequest =>
          applicationsConnector
            .generateDeployment(deploymentsRequest)
            .map {
              case response: SuccessfulDeploymentsResponse =>
                logger.info(s"Successful deployments response${System.lineSeparator()}${Json.prettyPrint(Json.toJson(response))}")
                Ok(Json.toJson(response))
              case response: InvalidOasResponse =>
                logger.info(s"Invalid OAS deployments response${System.lineSeparator()}${Json.prettyPrint(Json.toJson(response))}")
                BadRequest(Json.toJson(response))
            }
      )
  }

}

object SimpleApiDeploymentController {

  val deploymentsRequestForm: Form[DeploymentsRequest] = Form(
    mapping(
      "lineOfBusiness" -> text,
      "name" -> text,
      "description" -> text,
      "egress" -> text,
      "oas" -> text
    )(DeploymentsRequest.apply)(DeploymentsRequest.unapply)
  )

}
