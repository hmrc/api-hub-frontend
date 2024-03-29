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

import controllers.actions._
import forms.ConfirmAddTeamMemberFormProvider
import models.{CheckMode, Mode, NormalMode}
import navigation.Navigator
import pages.ConfirmAddTeamMemberPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.ConfirmAddTeamMember
import viewmodels.govuk.summarylist._
import views.html.ConfirmAddTeamMemberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmAddTeamMemberController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                sessionRepository: SessionRepository,
                                                navigator: Navigator,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: ConfirmAddTeamMemberFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: ConfirmAddTeamMemberView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = { (identify andThen getData andThen requireData) {
    implicit request =>
      val teamMemberDetails = SummaryListViewModel(
        rows = ConfirmAddTeamMember.rows(request.userAnswers)
      )
      request.userAnswers.get(ConfirmAddTeamMemberPage) match {
        case None => Ok(view(form, teamMemberDetails, Some(request.user), mode))
        case Some(value) => {
          Ok(view(mode match{
                      case NormalMode => form.fill(value)
                      case CheckMode => form
                    },
                  teamMemberDetails, Some(request.user), mode))
        }
      }
   }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val teamMemberDetails = SummaryListViewModel(
        rows = ConfirmAddTeamMember.rows(request.userAnswers)
      )
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, teamMemberDetails, Some(request.user), mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmAddTeamMemberPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ConfirmAddTeamMemberPage, mode, updatedAnswers))
      )
  }
}
