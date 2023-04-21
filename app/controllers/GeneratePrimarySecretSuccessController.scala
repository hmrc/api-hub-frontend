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

import config.FrontendAppConfig
import controllers.actions._
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Application, Credential, Secret}
import play.api.Logging

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.GeneratePrimarySecretSuccessViewModel
import views.html.GeneratePrimarySecretSuccessView

import scala.concurrent.{ExecutionContext, Future}

class GeneratePrimarySecretSuccessController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  val controllerComponents: MessagesControllerComponents,
  view: GeneratePrimarySecretSuccessView,
  apiHubService: ApiHubService,
  frontendAppConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id)).async {
    implicit request =>
      qaTechDeliveryValidPrimaryCredential(request.application) match {
        case Some(credential) =>
          val summaryList = GeneratePrimarySecretSuccessViewModel.buildSummary(
            request.application,
            frontendAppConfig.environmentNames,
            credential,
            Secret("top-secret")
          )

          Future.successful(Ok(view(summaryList, Some(request.identifierRequest.user))))

//          apiHubService.createPrimarySecret(id).map {
//            case Some(secret) =>
//              val summaryList = GeneratePrimarySecretSuccessViewModel.buildSummary(
//                request.application,
//                frontendAppConfig.environmentNames,
//                credential,
//                secret
//              )
//
//              Ok(view(summaryList, Some(request.identifierRequest.user)))
//            case _ =>
//              logger.warn(s"No primary secret generated for application $id")
//              BadRequest
//          }
        case None =>
          logger.warn(s"Cannot find valid primary credential for application $id")
          Future.successful(BadRequest)
      }
  }

  private def qaTechDeliveryValidPrimaryCredential(application: Application): Option[Credential] = {
    if (application.getPrimaryCredentials.length != 1) {
      None
    }
    else {
      application.getPrimaryCredentials
        .headOption
//        .filter(_.secretFragment.isEmpty)
    }
  }

}
