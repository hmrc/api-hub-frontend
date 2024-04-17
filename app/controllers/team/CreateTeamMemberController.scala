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
import forms.AddTeamMemberDetailsFormProvider
import models.NormalMode
import navigation.Navigator
import pages.{CreateTeamMemberPage, CreateTeamMembersPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.CreateTeamSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddTeamMemberDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateTeamMemberController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: CreateTeamSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: CreateTeamDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: AddTeamMemberDetailsFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: AddTeamMemberDetailsView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(view(form, routes.CreateTeamMemberController.onSubmit(), request.user))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, routes.CreateTeamMemberController.onSubmit(), request.user))),

        unformattedNewTeamMember => {
          val existingTeamMembers = request.userAnswers.get(CreateTeamMembersPage).getOrElse(Seq.empty)
          val newTeamMember = unformattedNewTeamMember.copy(email = formatEmailAddress(unformattedNewTeamMember.email))

          existingTeamMembers.find(existingTeamMember => emailAddressesMatch(existingTeamMember.email, newTeamMember.email)) match {
            case Some(_) =>
              val formWithErrors = form.fill(unformattedNewTeamMember).withError("email", "createTeamMember.email.duplicate")
              Future.successful(BadRequest(view(formWithErrors, routes.CreateTeamMemberController.onSubmit(), request.user)))
            case None =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CreateTeamMembersPage, existingTeamMembers :+ newTeamMember))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(CreateTeamMemberPage, NormalMode, updatedAnswers))
          }
        }
      )
  }

  private def emailAddressesMatch(email1: String, email2: String): Boolean = formatEmailAddress(email1) == formatEmailAddress(email2)

  private def formatEmailAddress(email: String): String = email.toLowerCase.trim

}
