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

package controllers.team

import controllers.actions._
import models.CheckMode
import models.team.NewTeam
import pages.{CreateTeamMembersPage, CreateTeamNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.CreateTeamSuccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateTeamController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        service: ApiHubService,
                                        sessionRepository: CreateTeamSessionRepository,
                                        getData: CreateTeamDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: CreateTeamSuccessView,
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(CreateTeamNamePage) match {
        case Some(teamName) =>
          val team = NewTeam(teamName, request.userAnswers.get(CreateTeamMembersPage).getOrElse(Seq.empty))
          service.createTeam(team)
            .flatMap(_ => sessionRepository.clear(request.userAnswers.id))
            .map(_ => Ok(view(Some(request.user))))

        case None =>
          Future.successful(Redirect(routes.CreateTeamNameController.onPageLoad(CheckMode)))
      }
  }

}
