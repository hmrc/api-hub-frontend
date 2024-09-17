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

package controllers.application

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AccessRequestDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.{ApplicationApiBuilder, ErrorResultBuilder}
import models.UserAnswers
import models.accessrequest.{AccessRequestApi, AccessRequestEndpoint, AccessRequestRequest}
import models.application.Application
import models.requests.DataRequest
import pages.{AccessRequestApplicationIdPage, ProvideSupportingInformationPage, RequestProductionAccessPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repositories.AccessRequestSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.application.Inaccessible
import views.html.application.RequestProductionAccessSuccessView

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
  applicationApiBuilder: ApplicationApiBuilder,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def submitRequest(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(request).fold(
        call => Future.successful(Redirect(call)),
        validated =>
          buildAccessRequest(validated._1, validated._2, validated._3)(request).flatMap(
            accessRequest =>
              apiHubService.requestProductionAccess(accessRequest)
                .flatMap(_ => sessionRepository.clear(request.user.userId))
                .flatMap(_ => Future.successful(Ok(requestProductionAccessSuccessView(validated._1, Some(request.user), accessRequest.apis))))
                .recoverWith {
                  case e: UpstreamErrorResponse if e.statusCode == BAD_GATEWAY => Future.successful(badGateway(e))
                }
          )
      )
  }

  private def validate(implicit request: DataRequest[AnyContent]): Either[Call, (Application, String, String)] = {
    for {
      application <- validateApplication(request.userAnswers)
      _ <- validateConditions(request.userAnswers)
      supportingInformation <- validateSupportingInformation(request.userAnswers)
    } yield (application, supportingInformation, request.user.email)
  }

  private def validateApplication(userAnswers: UserAnswers): Either[Call, Application] = {
    userAnswers.get(AccessRequestApplicationIdPage) match {
      case Some(application) => Right(application)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateConditions(userAnswers: UserAnswers): Either[Call, Unit] = {
    userAnswers.get(RequestProductionAccessPage) match {
      case Some(_) => Right(())
      case None => Left(controllers.application.routes.RequestProductionAccessController.onPageLoad())
    }
  }

  private def validateSupportingInformation(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ProvideSupportingInformationPage) match {
      case Some(information) => Right(information)
      case None => Left(controllers.application.routes.ProvideSupportingInformationController.onPageLoad())
    }
  }

  private def buildAccessRequest(application: Application, supportingInformation: String, requestedBy: String)(implicit request: DataRequest[AnyContent]): Future[AccessRequestRequest] = {
    applicationApiBuilder.build(application).map(
      applicationApis =>
        val accessRequestApis = applicationApis.filter(_.endpoints.exists(_.primaryAccess == Inaccessible)).map(applicationApi => {
          val accessRequestEndpoints = applicationApi.endpoints.filter(_.primaryAccess == Inaccessible).map(endpoint => AccessRequestEndpoint(endpoint.httpMethod, endpoint.path, endpoint.scopes))
          AccessRequestApi(
            applicationApi.apiId,
            applicationApi.apiTitle,
            accessRequestEndpoints
          )
        })
        AccessRequestRequest(application.id, supportingInformation, requestedBy, accessRequestApis)
    )
  }

  private def badGateway(t: Throwable)(implicit request: Request[?]): Result = {
    errorResultBuilder.internalServerError(Messages("addAnApiComplete.failed"), t)
  }

}
