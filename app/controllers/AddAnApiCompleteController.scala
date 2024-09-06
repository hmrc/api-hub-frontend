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

import controllers.actions.{AddAnApiCheckContextActionProvider, AddAnApiDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.api.ApiDetail
import models.application.Application
import models.{AddAnApiContext, ApiPolicyConditionsDeclaration, AvailableEndpoints, CheckMode, UserAnswers}
import pages.{AddAnApiApiPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage, ApiPolicyConditionsDeclarationPage}
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
  addAnApiSessionRepository: AddAnApiSessionRepository,
  checkContext: AddAnApiCheckContextActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import AddAnApiCompleteController._

  def addApi(context: AddAnApiContext): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(context)).async {
    implicit request =>
      request.userAnswers.get(AddAnApiApiPage) match {
        case Some(apiDetail) =>
          validate(request.userAnswers, context).fold(
            call => Future.successful(Redirect(call)),
            addAnApiRequest => {
              val selectedEndpoints = AvailableEndpoints.selectedEndpoints(apiDetail, addAnApiRequest.application, request.userAnswers).flatten(_._2).toSeq
              apiHubService.addApi(addAnApiRequest.application.id, addAnApiRequest.apiId, addAnApiRequest.apiTitle, selectedEndpoints)
            } flatMap {
              case Some(_) =>
                addAnApiSessionRepository.clear(request.user.userId).map(_ =>
                  Redirect(routes.AddAnApiSuccessController.onPageLoad(addAnApiRequest.application.id, addAnApiRequest.apiId))
                )
              case None => Future.successful(applicationNotFound(addAnApiRequest.application.id))
            } recoverWith {
              case e: UpstreamErrorResponse if e.statusCode == BAD_GATEWAY => Future.successful(badGateway(e))
            }
          )
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def validate(userAnswers: UserAnswers, context: AddAnApiContext): Either[Call, AddAnApiRequest] = {
    for {
      apiDetail <- validateApiId(userAnswers)
      application <- validateSelectedApplication(userAnswers)
      endpoints <- validateSelectedEndpoints(userAnswers, context)
      _ <- validatePolicyConditions(userAnswers, context)
    } yield AddAnApiRequest(application, apiDetail.id, apiDetail.title, endpoints)
  }

  private def validateApiId(userAnswers: UserAnswers): Either[Call, ApiDetail] = {
    userAnswers.get(AddAnApiApiPage) match {
      case Some(apiDetail) => Right(apiDetail)
      case None => Left(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateSelectedApplication(userAnswers: UserAnswers): Either[Call, Application] = {
    userAnswers.get(AddAnApiSelectApplicationPage) match {
      case Some(application) => Right(application)
      case None => Left(routes.AddAnApiSelectApplicationController.onPageLoad(CheckMode))
    }
  }

  private def validateSelectedEndpoints(userAnswers: UserAnswers, context: AddAnApiContext): Either[Call, Set[String]] = {
    userAnswers.get(AddAnApiSelectEndpointsPage) match {
      case Some(endpoints) if endpoints.nonEmpty => Right(endpoints.flatten)
      case _ => Left(routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode, context))
    }
  }

  private def validatePolicyConditions(userAnswers: UserAnswers, context: AddAnApiContext): Either[Call, Unit] = {
    userAnswers.get(ApiPolicyConditionsDeclarationPage) match {
      case Some(policyConditions) if policyConditions == Set(ApiPolicyConditionsDeclaration.Accept) => Right(())
      case _ => Left(routes.ApiPolicyConditionsDeclarationPageController.onPageLoad(CheckMode, context))
    }
  }

  private def applicationNotFound(applicationId: String)(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      heading = Messages("site.applicationNotFoundHeading"),
      message = Messages("site.applicationNotFoundMessage", applicationId)
    )
  }

  private def badGateway(t: Throwable)(implicit request: Request[?]): Result = {
    errorResultBuilder.internalServerError(Messages("addAnApiComplete.failed"), t)
  }

}

object AddAnApiCompleteController {

  case class AddAnApiRequest(application: Application, apiId: String, apiTitle: String, scopes: Set[String])

}
