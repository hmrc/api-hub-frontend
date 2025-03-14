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

package controllers.application.cancelaccessrequest

import com.google.inject.Inject
import controllers.actions.{CancelAccessRequestDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.accessrequest.AccessRequest
import models.application.Application
import models.requests.{BaseRequest, DataRequest}
import models.user.UserModel
import models.{CheckMode, UserAnswers}
import pages.application.cancelaccessrequest.{CancelAccessRequestApplicationPage, CancelAccessRequestConfirmPage, CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import repositories.CancelAccessRequestSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.cancelaccessrequest.CancelAccessRequestSuccessView
import config.HipEnvironments
import viewmodels.application.AccessRequestsByEnvironment

import scala.concurrent.{ExecutionContext, Future}

class CancelAccessRequestEndJourneyController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  sessionRepository: CancelAccessRequestSessionRepository,
  identify: IdentifierAction,
  getData: CancelAccessRequestDataRetrievalAction,
  requireData: DataRequiredAction,
  cancelAccessRequestSuccessView: CancelAccessRequestSuccessView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import CancelAccessRequestEndJourneyController.*

  def submitRequest(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      validate(request).fold(
        call => Future.successful(Redirect(call)),
        data => {
          val accessRequests = getCancellableRequests(data)
          Future.sequence(getCancellableRequests(data).map(accessRequest => apiHubService.cancelAccessRequest(accessRequest.id, request.user.email)))
            .flatMap(_ => sessionRepository.clear(request.user.userId))
            .map(_ => Ok(cancelAccessRequestSuccessView(data.application, Some(request.user), AccessRequestsByEnvironment(accessRequests, hipEnvironments))))
            .recoverWith {
              case e: UpstreamErrorResponse if e.statusCode == BAD_GATEWAY => Future.successful(badGateway(e))
            }
        }
      )
    }
  }

  private def cancelAccessRequests(data: CancelAccessRequestEndJourneyController.Data, user: UserModel)(implicit hc:HeaderCarrier): Future[Seq[Option[Unit]]] = {
    Future.sequence(getCancellableRequests(data).map(request => apiHubService.cancelAccessRequest(request.id, user.email)))
  }

  private def getCancellableRequests(data: CancelAccessRequestEndJourneyController.Data) = {
    data.accessRequests.filter(accessRequest => data.selectedAccessRequests.contains(accessRequest.id))
  }

  private def validate(request: DataRequest[?]): Either[Call, Data] = {
    for {
      application <- validateApplication(request.userAnswers)
      accessRequests <- validateAccessRequests(request.userAnswers)
      apis <- validateApis(request.userAnswers)
      _ <- validateDeclaration(request.userAnswers)
    } yield Data(application, accessRequests, apis)
  }

  private def validateApplication(userAnswers: UserAnswers): Either[Call, Application] = {
    userAnswers.get(CancelAccessRequestApplicationPage) match {
      case Some(application) => Right(application)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateAccessRequests(userAnswers: UserAnswers): Either[Call, Seq[AccessRequest]] = {
    userAnswers.get(CancelAccessRequestPendingPage) match {
      case Some(accessRequests) => Right(accessRequests)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateApis(userAnswers: UserAnswers): Either[Call, Set[String]] = {
    userAnswers.get(CancelAccessRequestSelectApiPage) match {
      case Some(applicationApis) => Right(applicationApis)
      case None => Left(controllers.application.cancelaccessrequest.routes.CancelAccessRequestSelectApiController.onPageLoad(CheckMode))
    }
  }

  private def validateDeclaration(userAnswers: UserAnswers): Either[Call, Unit] = {
    userAnswers.get(CancelAccessRequestConfirmPage) match {
      case Some(_) => Right(())
      case None => Left(controllers.application.cancelaccessrequest.routes.CancelAccessRequestConfirmController.onPageLoad(CheckMode))
    }
  }

  private def badGateway(t: Throwable)(implicit request: BaseRequest[?]): Result = {
    errorResultBuilder.internalServerError(Messages("addAnApiComplete.failed"), t)
  }

}

object CancelAccessRequestEndJourneyController {

  case class Data(
                   application: Application,
                   accessRequests: Seq[AccessRequest],
                   selectedAccessRequests: Set[String]
  )
}
