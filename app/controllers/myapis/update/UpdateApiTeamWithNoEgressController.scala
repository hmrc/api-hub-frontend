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

package controllers.myapis.update

import controllers.actions.*
import controllers.routes
import models.Mode
import navigation.Navigator
import pages.myapis.update.{UpdateApiTeamPage, UpdateApiTeamWithNoEgressPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.ProduceApiTeamWithNoEgressViewModel
import views.html.myapis.produce.ProduceApiTeamWithNoEgressView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UpdateApiTeamWithNoEgressController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: ProduceApiSessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: UpdateApiDataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ProduceApiTeamWithNoEgressView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

    
  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(UpdateApiTeamPage).fold(
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      )(team => Ok(view(
        ProduceApiTeamWithNoEgressViewModel(
          team,
          request.user,
          controllers.myapis.update.routes.UpdateApiTeamWithNoEgressController.onSubmit(mode),
          "myApis.produce.teamHasNoEgress.body.update.1"
        )
      )))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(UpdateApiTeamWithNoEgressPage, mode, request.userAnswers))
  }
}
