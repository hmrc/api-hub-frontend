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

package controllers.admin

import com.google.inject.{Inject, Singleton}
import config.HipEnvironments
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.admin.{ApimRequests, TestApimEndpointsViewModel}
import views.html.admin.TestApimEndpointsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestApimEndpointsController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  hipEnvironments: HipEnvironments,
  view: TestApimEndpointsView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport) {
    implicit request =>
      Ok(view(TestApimEndpointsViewModel(hipEnvironments), request.user))
  }

  def callApim[T](environment: String, endpointId: String, params: String): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      (for {
        hipEnvironment <- hipEnvironments.forEnvironmentIdOptional(environment)
        apimRequest <- ApimRequests.requests.find(_.id == endpointId)
      } yield apiHubService.testApimEndpoint(hipEnvironment, apimRequest, params)) match {
        case Some(f) => f.map(response => Ok(response))
        case None => Future.successful(BadRequest)
      }
  }

}
