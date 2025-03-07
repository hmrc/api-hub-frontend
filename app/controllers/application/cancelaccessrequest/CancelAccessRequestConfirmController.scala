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

import config.{HipEnvironment, HipEnvironments}
import controllers.actions.*
import controllers.routes
import forms.application.cancelaccessrequest.CancelAccessRequestConfirmFormProvider
import models.Mode
import models.accessrequest.AccessRequest
import models.requests.DataRequest
import navigation.Navigator
import pages.application.cancelaccessrequest.{CancelAccessRequestConfirmPage, CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{CancelAccessRequestSessionRepository, SessionRepository}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.cancelaccessrequest.CancelAccessRequestConfirmView
import viewmodels.application.AccessRequestsByEnvironment
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CancelAccessRequestConfirmController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: CancelAccessRequestSessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: CancelAccessRequestDataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: CancelAccessRequestConfirmFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: CancelAccessRequestConfirmView,
                                         hipEnvironments: HipEnvironments
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) async {
    implicit request => {
      (request.userAnswers.get(CancelAccessRequestPendingPage), request.userAnswers.get(CancelAccessRequestSelectApiPage)) match {
        case (_, None) => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case (None, _) => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case (Some(accessRequests), Some(apiIds)) =>
          val preparedForm = request.userAnswers.get(CancelAccessRequestConfirmPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          val accessRequests = AccessRequestsByEnvironment(getCancellableRequests(request), hipEnvironments)
          Future.successful(Ok(view(preparedForm, mode, accessRequests, request.user)))
      }
    }
  }

  private def getCancellableRequests(request: DataRequest[AnyContent]) = {
    val pendingAccessRequests = request.userAnswers.get(CancelAccessRequestPendingPage).getOrElse(Seq.empty)
    val apisToCancel = request.userAnswers.get(CancelAccessRequestSelectApiPage).getOrElse(Set.empty)
    val accessRequestsToCancel = pendingAccessRequests.filter(accessRequest => apisToCancel.exists(_.equals(accessRequest.id)))
    accessRequestsToCancel
  }
  
  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
      implicit request => {
        (request.userAnswers.get(CancelAccessRequestPendingPage),
          request.userAnswers.get(CancelAccessRequestSelectApiPage)) match {
          case (_, None) => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case (None, _) => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case (Some(accessRequests), Some(apiIds)) =>
            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, AccessRequestsByEnvironment(accessRequests, hipEnvironments), request.user))),

              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CancelAccessRequestConfirmPage, value))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(CancelAccessRequestConfirmPage, mode, updatedAnswers))
            )
        }
      }
  }
}