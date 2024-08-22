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

package controllers.team

import com.google.inject.Inject
import controllers.actions.{CreateTeamDataRetrievalAction, DataRequiredAction, IdentifierAction}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{CreateTeamAddTeamMemberSummary, CreateTeamNameSummary}
import viewmodels.govuk.summarylist._
import views.html.team.CreateTeamCheckYourAnswersView

class CreateTeamCheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: CreateTeamDataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CreateTeamCheckYourAnswersView
                                          ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val teamName = SummaryListViewModel(
        rows = Seq(
          CreateTeamNameSummary.row(request.userAnswers)
        ).flatten
      )

      val teamMemberDetails = CreateTeamAddTeamMemberSummary.summary(request.userAnswers)

      Ok(view(teamName, teamMemberDetails, Some(request.user)))
  }
}
