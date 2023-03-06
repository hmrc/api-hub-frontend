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
import forms.AddTeamMemberDetailsFormProvider
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import models.application.TeamMember
import navigation.Navigator
import pages.TeamMembersPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddTeamMemberDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddTeamMemberDetailsController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                sessionRepository: SessionRepository,
                                                navigator: Navigator,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: AddTeamMemberDetailsFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: AddTeamMemberDetailsView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode, index: Int): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      validate(mode, index, request.userAnswers).fold(
        result => result,
        teamMembers => Ok(view(prepareForm(index, teamMembers), mode, index))
      )
  }

  def onSubmit(mode: Mode, index: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(mode, index, request.userAnswers).fold(
        result => Future.successful(result),
        teamMembers => {
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, index))),

            email => {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(TeamMembersPage, updateTeamMembers(email, index, teamMembers)))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(TeamMembersPage, mode, updatedAnswers))
            }
          )
        }
      )
  }

  private def validate(mode: Mode, index: Int, userAnswers: UserAnswers): Either[Result, Seq[TeamMember]] = {
    (mode, index) match {
      case (NormalMode, 0) => Right(userAnswers.get(TeamMembersPage).getOrElse(Seq.empty))
      case (CheckMode, i) if i >= 1 =>
        userAnswers.get(TeamMembersPage) match {
          case Some(teamMembers) if i <= teamMembers.length => Right(teamMembers)
          case _ => Left(NotFound)
        }
      case _ => Left(NotFound)
    }
  }

  private def prepareForm(index: Int, teamMembers: Seq[TeamMember]): Form[String] = {
    if (index > 0) {
      form.fill(teamMembers(index - 1).email)
    }
    else {
      form
    }
  }

  private def updateTeamMembers(email: String, index: Int, teamMembers: Seq[TeamMember]): Seq[TeamMember] = {
    index match {
      case i if i > 0 => teamMembers
        .zipWithIndex
        .map {
          case (_, j) if j == i - 1 => TeamMember(email)
          case (teamMember, _) => teamMember
        }
      case _ => teamMembers :+ TeamMember(email)
    }
  }

}
