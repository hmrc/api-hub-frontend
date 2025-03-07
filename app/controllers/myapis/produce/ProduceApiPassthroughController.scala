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

import controllers.actions._
import forms.myapis.produce.ProduceApiPassthroughFormProvider
import models.Mode
import navigation.Navigator
import pages.myapis.produce.ProduceApiPassthroughPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiPassthroughView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiPassthroughController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: ProduceApiSessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         isSupport: AuthorisedSupportAction,
                                         getData: ProduceApiDataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: ProduceApiPassthroughFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: ProduceApiPassthroughView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiPassthroughPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, request.user))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, request.user))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiPassthroughPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiPassthroughPage, mode, updatedAnswers))
      )
  }
}
