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

import controllers.actions.IdentifierAction
import models.application.TeamMember
import models.{NormalMode, UserAnswers}
import pages.TeamMembersPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 identify: IdentifierAction,
                                 sessionRepository: SessionRepository,
                                 view: IndexView,
                                 apiHubService: ApiHubService
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    apiHubService.getApplications() map {
      applications =>
        Ok(view(applications, Some(request.user)))
    }
  }

  def onSubmit: Action[AnyContent] = identify.async { implicit request =>
    request.user.email.fold[Future[Result]] {
      logger.warn("Current user has no email address")
      Future.successful(InternalServerError)
    }(
      email =>
        for {
          userAnswers <- Future.fromTry(UserAnswers(request.user.userId).set(TeamMembersPage, Seq(TeamMember(email))))
          _ <- sessionRepository.set(userAnswers)
        } yield Redirect(routes.ApplicationNameController.onPageLoad(NormalMode))
    )
  }

    def createApplication: Action[AnyContent] = identify.async { implicit request =>
    request.user.email.fold[Future[Result]] {
      logger.warn("Current user has no email address")
      Future.successful(InternalServerError)
    }(
      email =>
        for {
          userAnswers <- Future.fromTry(UserAnswers(request.user.userId).set(TeamMembersPage, Seq(TeamMember(email))))
          _ <- sessionRepository.set(userAnswers)
        } yield Redirect(routes.ApplicationNameController.onPageLoad(NormalMode))
    )
  }



}
