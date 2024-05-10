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
import forms.CreateTeamNameFormProvider
import models.Mode
import navigation.Navigator
import pages.CreateTeamNamePage
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.team.CreateTeamNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateTeamNameController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: CreateTeamSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: CreateTeamDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: CreateTeamNameFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: CreateTeamNameView,
                                        apiHubService: ApiHubService
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(CreateTeamNamePage) match {
        case None => Future.successful(Ok(view(form, mode)))
        case Some(name) =>
          val preparedForm = form.fill(name)

          isUniqueName(name).map(
            isUnique =>
              if (isUnique) {
                Ok(view(preparedForm, mode))
              }
              else {
                nameNotUnique(preparedForm, mode)
              }
          )
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        name =>
          isUniqueName(name).flatMap(
            isUnique =>
              if (isUnique) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CreateTeamNamePage, name))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(CreateTeamNamePage, mode, updatedAnswers))
              }
              else {
                Future.successful(nameNotUnique(form.fill(name), mode))
              }
          )
      )
  }

  private def isUniqueName(name: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    apiHubService.findTeamByName(name).map(_.isEmpty)
  }

  private def nameNotUnique(form: Form[String], mode: Mode)(implicit request: Request[_]) = {
    BadRequest(view(form.withError(FormError("value", "createTeamName.error.nameNotUnique")), mode))
  }

}
