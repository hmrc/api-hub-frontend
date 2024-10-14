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

package controllers.application.cancelaccessrequest

import controllers.actions.*
import forms.application.cancelaccessrequest.CancelAccessRequestSelectApiFormProvider
import models.Mode
import navigation.Navigator
import pages.application.cancelaccessrequest.{CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.CancelAccessRequestSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.cancelaccessrequest.CancelAccessRequestSelectApiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CancelAccessRequestSelectApiController @Inject()(
  override val messagesApi: MessagesApi,
  sessionRepository: CancelAccessRequestSessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: CancelAccessRequestDataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: CancelAccessRequestSelectApiFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: CancelAccessRequestSelectApiView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(CancelAccessRequestPendingPage) match {
        case Some(accessRequests) =>
          val form = formProvider(accessRequests)

          val preparedForm = request.userAnswers.get(CancelAccessRequestSelectApiPage) match {
            case None => {
              Console.println("OIYAF: Nothing")
              form
            }
            case Some(value) => {
              Console.println(s"OIYAF: $value")
              form.fill(value)
            }
          }

          Ok(view(accessRequests, preparedForm, mode, request.user))
        case None =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(CancelAccessRequestPendingPage) match {
        case Some(accessRequests) =>
          val form = formProvider(accessRequests)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(accessRequests, formWithErrors, mode, request.user))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CancelAccessRequestSelectApiPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(CancelAccessRequestSelectApiPage, mode, updatedAnswers))
          )
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

}
