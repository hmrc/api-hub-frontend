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
import models.{ApiPolicyConditionsDeclaration, CheckMode, UserAnswers}
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage, ApiPolicyConditionsDeclarationPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import services.ApiHubService
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
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import AddAnApiCompleteController._

  def addApi(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        addAnApiRequest => apiHubService.addScopes(addAnApiRequest.applicationId, addAnApiRequest.scopes) map {
          case Some(_) => Redirect(routes.IndexController.onPageLoad)
          case None => applicationNotFound(addAnApiRequest.applicationId)
        }
      )
  }

  private def validate(userAnswers: UserAnswers): Either[Call, AddAnApiRequest] = {
    for {
      _ <- validateApiId(userAnswers)
      applicationId <- validateSelectedApplication(userAnswers)
      endpoints <- validateSelectedEndpoints(userAnswers)
      _ <- validatePolicyConditions(userAnswers)
    } yield AddAnApiRequest(applicationId, endpoints)
  }

  private def validateApiId(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(AddAnApiApiIdPage) match {
      case Some(apiId) => Right(apiId)
      case None => Left(routes.HipApisController.onPageLoad())
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
      case Some(endpoints) => Right(endpoints.flatten)
      case None => Left(routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode))
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

}

object AddAnApiCompleteController {

  case class AddAnApiRequest(applicationId: String, scopes: Set[String])

}
