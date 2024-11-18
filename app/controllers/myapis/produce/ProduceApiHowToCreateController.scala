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

package controllers.myapis.produce

import controllers.actions.*
import forms.myapis.produce.ProduceApiHowToCreateFormProvider
import models.Mode
import models.myapis.produce.ProduceApiHowToCreate
import models.requests.DataRequest
import models.user.UserModel
import navigation.Navigator
import pages.myapis.produce.ProduceApiHowToCreatePage
import pages.myapis.update.UpdateApiHowToUpdatePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{ProduceApiSessionRepository, SessionRepository}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.ProduceApiHowToCreateViewModel
import views.html.myapis.produce.ProduceApiHowToCreateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiHowToCreateController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: ProduceApiSessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: ProduceApiDataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: ProduceApiHowToCreateFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ProduceApiHowToCreateView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()
    
  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiHowToCreatePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Future.successful(Ok(buildView(preparedForm, mode, request.user)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(buildView(formWithErrors, mode, request.user))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiHowToCreatePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiHowToUpdatePage, mode, updatedAnswers))
      )
  }

  private def buildView(form: Form[ProduceApiHowToCreate], mode: Mode, user: UserModel)(implicit request: DataRequest[AnyContent]) = {
    val viewModel = ProduceApiHowToCreateViewModel(
      "myApis.produce.howtocreate.title",
      "myApis.produce.howtocreate.heading",
      None,
      controllers.myapis.produce.routes.ProduceApiHowToCreateController.onSubmit(mode))
    view(form, viewModel, user)
  }
}
