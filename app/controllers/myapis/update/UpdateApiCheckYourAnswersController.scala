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

package controllers.myapis.update

import controllers.actions.*
import controllers.myapis.update.routes
import models.Mode
import models.deployment.SuccessfulDeploymentsResponse
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.DeploymentSuccessView
import views.html.myapis.produce.ProduceApiCheckYourAnswersView
import viewmodels.govuk.all.SummaryListViewModel
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiCheckYourAnswersController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ProduceApiCheckYourAnswersView,
                                        successView: DeploymentSuccessView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identify {
    implicit request =>  Ok(view(SummaryListViewModel(Seq.empty), request.user, None))
  }

  def onSubmit(): Action[AnyContent] = identify {
    implicit request => Ok(successView(request.user, "publisher-reference", "name"))
  }
}
