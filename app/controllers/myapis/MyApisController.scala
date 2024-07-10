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

package controllers.myapis

import com.google.inject.{Inject, Singleton}
import controllers.actions.IdentifierAction
import controllers.helpers.ErrorResultBuilder
import models.api.ApiDetail
import models.application.TeamMember
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.MyApisView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyApisController @Inject()(
                                  override val controllerComponents: MessagesControllerComponents,
                                  apiHubService: ApiHubService,
                                  view: MyApisView,
                                  errorResultBuilder: ErrorResultBuilder,
                                  identified: IdentifierAction
                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identified.async {
    implicit request =>
      val eventualDetails = request.user.email.map(email => apiHubService.getUserApis(TeamMember(email))).getOrElse(Future.successful(Seq.empty))
      eventualDetails flatMap {
        case apiDetails: Seq[ApiDetail] if apiDetails.isEmpty =>
          Future.successful(errorResultBuilder.notFound(
            Messages("myApis.empty.heading")
          ))
        case apiDetails: Seq[ApiDetail] =>
          Future.successful(Ok(view(apiDetails.sortWith( _.title.toUpperCase < _.title.toUpperCase), request.user)))
      }
  }
}
