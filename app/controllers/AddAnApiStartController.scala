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

import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.api.ApiDetail
import models.requests.IdentifierRequest
import models.{AddAnApi, AddAnApiContext, AddEndpoints, AvailableEndpoints, NormalMode, UserAnswers}
import navigation.Navigator
import pages.{AddAnApiApiPage, AddAnApiContextPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AddAnApiStartController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  apiHubService: ApiHubService,
  addAnApiSessionRepository: AddAnApiSessionRepository,
  clock: Clock,
  navigator: Navigator,
  errorResultBuilder: ErrorResultBuilder,
  applicationAuth: ApplicationAuthActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def addAnApi(apiId: String): Action[AnyContent] = identify.async {
    implicit request =>
      fetchApiDetail(apiId).flatMap {
        case Right(apiDetail) =>
          startJourney(commonUserAnswers(apiDetail, AddAnApi, request))
        case Left(result) => Future.successful(result)
      }
  }

  def addEndpoints(applicationId: String, apiId: String): Action[AnyContent] = (identify andThen applicationAuth(applicationId)).async {
    implicit request =>
      fetchApiDetail(apiId).flatMap {
        case Right(apiDetail) =>
          startJourney(
            commonUserAnswers(apiDetail, AddEndpoints, request.identifierRequest)
              .flatMap(_.set(AddAnApiSelectApplicationPage, request.application))
              .flatMap(_.set(AddAnApiSelectEndpointsPage, AvailableEndpoints.addedScopes(apiDetail, request.application)))
          )
        case Left(result) => Future.successful(result)
      }
  }

  private def fetchApiDetail(apiId: String)(implicit request: Request[?]): Future[Either[Result, ApiDetail]] = {
    apiHubService.getApiDetail(apiId).map {
      case Some(apiDetail) => Right(apiDetail)
      case _ => Left(apiNotFound(apiId))
    }
  }

  private def commonUserAnswers(apiDetail: ApiDetail, context: AddAnApiContext, request: IdentifierRequest[?]): Try[UserAnswers] = {
    UserAnswers(
      id = request.user.userId,
      lastUpdated = clock.instant()
    )
      .set(AddAnApiApiPage, apiDetail)
      .flatMap(_.set(AddAnApiContextPage, context))
  }

  private def startJourney(userAnswers: Try[UserAnswers]) = {
    for {
      userAnswers <- Future.fromTry(userAnswers)
      _           <- addAnApiSessionRepository.set(userAnswers)
    } yield Redirect(navigator.nextPage(AddAnApiApiPage, NormalMode, userAnswers))
  }

  private def apiNotFound(apiId: String)(implicit request: Request[?]): Result = {
    errorResultBuilder.notFound(
      Messages("site.apiNotFound.heading"),
      Messages("site.apiNotFound.message", apiId)
    )
  }

}
