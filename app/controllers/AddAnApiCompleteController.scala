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

package controllers

import controllers.actions.{AddAnApiDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.{ApiPolicyConditionsDeclaration, AvailableEndpoints, CheckMode, UserAnswers}
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage, ApiPolicyConditionsDeclarationPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddAnApiCompleteController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: AddAnApiDataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            apiHubService: ApiHubService,
                                            errorResultBuilder: ErrorResultBuilder,
                                            addAnApiSessionRepository: AddAnApiSessionRepository
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import AddAnApiCompleteController._

  def addApi(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(AddAnApiApiIdPage) match {
        case Some(apiId) =>
          apiHubService.getApiDetail(apiId).flatMap {
            case Some(apiDetail) =>
              validate(request.userAnswers).fold(
                call => Future.successful(Redirect(call)),
                addAnApiRequest => {
                  val selectedEndpoints = AvailableEndpoints.selectedEndpoints(apiDetail, request.userAnswers).flatten(_._2).toSeq
                  apiHubService.addApi(addAnApiRequest.applicationId, addAnApiRequest.apiId, selectedEndpoints)
                } flatMap {
                  case Some(_) =>
                    addAnApiSessionRepository.clear(request.user.userId).map(_ =>
                      Redirect(routes.AddAnApiSuccessController.onPageLoad(addAnApiRequest.applicationId, addAnApiRequest.apiId))
                    )
                  case None => Future.successful(applicationNotFound(addAnApiRequest.applicationId))
                } recoverWith {
                  case e: UpstreamErrorResponse if e.statusCode == BAD_GATEWAY => Future.successful(badGateway(e))
                }
              )
            case None => apiNotFound(apiId)
          }
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }

  }

  private def apiNotFound(apiId: String)(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.notFound(
        Messages("site.apiNotFound.heading"),
        Messages("site.apiNotFound.message", apiId)
      )
    )
  }
  private def validate(userAnswers: UserAnswers): Either[Call, AddAnApiRequest] = {
    for {
      apiId <- validateApiId(userAnswers)
      applicationId <- validateSelectedApplication(userAnswers)
      endpoints <- validateSelectedEndpoints(userAnswers)
      _ <- validatePolicyConditions(userAnswers)
    } yield AddAnApiRequest(applicationId, apiId, endpoints)
  }

  private def validateApiId(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(AddAnApiApiIdPage) match {
      case Some(apiId) => Right(apiId)
      case None => Left(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateSelectedApplication(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(AddAnApiSelectApplicationPage) match {
      case Some(applicationId) => Right(applicationId)
      case None => Left(routes.AddAnApiSelectApplicationController.onPageLoad(CheckMode))
    }
  }

  private def validateSelectedEndpoints(userAnswers: UserAnswers): Either[Call, Set[String]] = {
    userAnswers.get(AddAnApiSelectEndpointsPage) match {
      case Some(endpoints) if endpoints.nonEmpty => Right(endpoints.flatten)
      case _ => Left(routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode))
    }
  }

  private def validatePolicyConditions(userAnswers: UserAnswers): Either[Call, Unit] = {
    userAnswers.get(ApiPolicyConditionsDeclarationPage) match {
      case Some(policyConditions) if policyConditions == Set(ApiPolicyConditionsDeclaration.Accept) => Right(())
      case _ => Left(routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(CheckMode))
    }
  }

  private def applicationNotFound(applicationId: String)(implicit request: Request[_]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.applicationNotFoundHeading"),
      message = Messages("site.applicationNotFoundMessage", applicationId)
    )
  }

  private def badGateway(t: Throwable)(implicit request: Request[_]): Result = {
    errorResultBuilder.internalServerError(Messages("addAnApiComplete.failed"), t)
  }

}

object AddAnApiCompleteController {

  case class AddAnApiRequest(applicationId: String, apiId: String, scopes: Set[String])

}
