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

package controllers.application.accessrequest

import com.google.inject.{Inject, Singleton}
import config.HipEnvironments
import controllers.actions.{AccessRequestDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.{CheckMode, UserAnswers}
import models.accessrequest.{AccessRequestApi, AccessRequestEndpoint, AccessRequestRequest}
import models.application.Application
import models.requests.{BaseRequest, DataRequest}
import pages.application.accessrequest.*
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.application.{ApplicationApi, Inaccessible}
import views.html.application.accessrequest.RequestProductionAccessSuccessView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RequestProductionAccessEndJourneyController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  sessionRepository: AccessRequestSessionRepository,
  identify: IdentifierAction,
  getData: AccessRequestDataRetrievalAction,
  requireData: DataRequiredAction,
  requestProductionAccessSuccessView: RequestProductionAccessSuccessView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  hipEnvironments: HipEnvironments,
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import RequestProductionAccessEndJourneyController.*

  def submitRequest(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(request).fold(
        call => Future.successful(Redirect(call)),
        data =>
          hipEnvironments.forUrlPathParameter(data.environmentId) match {
            case Some(hipEnvironment) if hipEnvironment.isProductionLike =>
              val accessRequest = data.toRequest(request.user.email)

              apiHubService.requestProductionAccess(accessRequest)
                .flatMap(_ => sessionRepository.clear(request.user.userId))
                .flatMap(_ => Future.successful(Ok(requestProductionAccessSuccessView(data.application, Some(request.user), accessRequest.apis, hipEnvironment))))
                .recoverWith {
                  case e: UpstreamErrorResponse if e.statusCode == BAD_GATEWAY => Future.successful(badGateway(e))
                }
            case _ => Future.successful(errorResultBuilder.notFound())
          }
    )
  }

  private def validate(request: DataRequest[?]): Either[Call, Data] = {
    for {
      application <- validateApplication(request.userAnswers)
      applicationApis <- validateApis(request.userAnswers)
      environmentId <- validateEnvironmentId(request.userAnswers)
      selectedApis <- validateSelectedApis(request.userAnswers)
      supportingInformation <- validateSupportingInformation(request.userAnswers)
      _ <- validateDeclaration(request.userAnswers)
    } yield Data(application, applicationApis, environmentId, selectedApis, supportingInformation)
  }

  private def validateApplication(userAnswers: UserAnswers): Either[Call, Application] = {
    userAnswers.get(RequestProductionAccessApplicationPage) match {
      case Some(application) => Right(application)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateApis(userAnswers: UserAnswers): Either[Call, Seq[ApplicationApi]] = {
    userAnswers.get(RequestProductionAccessApisPage) match {
      case Some(applicationApis) => Right(applicationApis)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateEnvironmentId(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(RequestProductionAccessEnvironmentIdPage) match {
      case Some(environmentId) => Right(environmentId)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateSelectedApis(userAnswers: UserAnswers): Either[Call, Set[String]] = {
    userAnswers.get(RequestProductionAccessSelectApisPage) match {
      case Some(selectedApis) => Right(selectedApis)
      case None => Left(controllers.application.accessrequest.routes.RequestProductionAccessSelectApisController.onPageLoad(CheckMode))
    }
  }

  private def validateSupportingInformation(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ProvideSupportingInformationPage) match {
      case Some(information) => Right(information)
      case None => Left(controllers.application.accessrequest.routes.ProvideSupportingInformationController.onPageLoad(CheckMode))
    }
  }

  private def validateDeclaration(userAnswers: UserAnswers): Either[Call, Unit] = {
    userAnswers.get(RequestProductionAccessPage) match {
      case Some(_) => Right(())
      case None => Left(controllers.application.accessrequest.routes.RequestProductionAccessController.onPageLoad())
    }
  }

  private def badGateway(t: Throwable)(implicit request: BaseRequest[?]): Result = {
    errorResultBuilder.internalServerError(Messages("addAnApiComplete.failed"), t)
  }

}

object RequestProductionAccessEndJourneyController {

  case class Data(
    application: Application,
    applicationApis: Seq[ApplicationApi],
    environmentId: String,
    selectedApis: Set[String],
    supportingInformation: String
  ) {

    def toRequest(requestedBy: String): AccessRequestRequest = {

      AccessRequestRequest(
        applicationId = application.id,
        supportingInformation = supportingInformation,
        requestedBy = requestedBy,
        apis = applicationApis
          .filter(
            applicationApi => selectedApis.exists(_.equals(applicationApi.apiId))
          )
          .map(
            applicationApi => AccessRequestApi(
              apiId = applicationApi.apiId,
              apiName = applicationApi.apiTitle,
              endpoints = applicationApi.endpoints
                .filter(_.productionAccess.equals(Inaccessible))
                .map(
                  endpoint => AccessRequestEndpoint(
                    httpMethod = endpoint.httpMethod,
                    path = endpoint.path,
                    scopes = endpoint.scopes
                  )
                )
            )
          ),
        environmentId = environmentId
      )
    }
  }
}
