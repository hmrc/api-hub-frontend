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

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AccessRequestDataRetrievalAction, CancelAccessRequestDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.accessrequest.{AccessRequest, AccessRequestApi, AccessRequestEndpoint, AccessRequestRequest}
import models.application.Application
import models.requests.DataRequest
import models.user.UserModel
import models.{CheckMode, NormalMode, UserAnswers}
import pages.application.accessrequest.*
import pages.application.cancelaccessrequest.{CancelAccessRequestApplicationPage, CancelAccessRequestConfirmPage, CancelAccessRequestPendingPage, CancelAccessRequestSelectApiPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import repositories.{AccessRequestSessionRepository, CancelAccessRequestSessionRepository}
import services.ApiHubService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.application.{ApplicationApi, Inaccessible}
import views.html.application.accessrequest.RequestProductionAccessSuccessView
import views.html.application.cancelaccessrequest.CancelAccessRequestSuccessView

import scala.concurrent.{ExecutionContext, Future}

class CancelAccessRequestEndJourneyController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  sessionRepository: CancelAccessRequestSessionRepository,
  identify: IdentifierAction,
  getData: CancelAccessRequestDataRetrievalAction,
  requireData: DataRequiredAction,
  cancelAccessRequestSuccessView: CancelAccessRequestSuccessView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import CancelAccessRequestEndJourneyController.*

  def submitRequest(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(request).fold(
        call => Future.successful(Redirect(call)),
        data => {
          val accessRequests = getCancellableRequests(data)
          Future.sequence(getCancellableRequests(data).map(accessRequest => apiHubService.cancelAccessRequest(accessRequest.id, request.user.email)))
            .flatMap(_ => sessionRepository.clear(request.user.userId))
            .flatMap(_ => Future.successful(Ok(cancelAccessRequestSuccessView(data.application, Some(request.user), accessRequests))))
            .recoverWith {
              case e: UpstreamErrorResponse if e.statusCode == BAD_GATEWAY => Future.successful(badGateway(e))
            }
        }
      )
  }

  private def cancelAccessRequests(data: CancelAccessRequestEndJourneyController.Data, user: UserModel)(implicit hc:HeaderCarrier): Future[Seq[Option[Unit]]] = {
    Future.sequence(getCancellableRequests(data).map(request => apiHubService.cancelAccessRequest(request.id, user.email)))
  }

  private def getCancellableRequests(data: CancelAccessRequestEndJourneyController.Data) = {
    data.accessRequests.filter(accessRequest => data.apis.contains(accessRequest.apiId))
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
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateDeclaration(userAnswers: UserAnswers): Either[Call, Unit] = {
    userAnswers.get(CancelAccessRequestConfirmPage) match {
      case Some(_) => Right(())
      case None => Left(controllers.application.cancelaccessrequest.routes.CancelAccessRequestConfirmController.onPageLoad(NormalMode))
    }
  }

  private def badGateway(t: Throwable)(implicit request: Request[?]): Result = {
    errorResultBuilder.internalServerError(Messages("addAnApiComplete.failed"), t)
  }

}

object CancelAccessRequestEndJourneyController {

  case class Data(
    application: Application,
    accessRequests: Seq[AccessRequest],
    apis: Set[String]
  )
}
