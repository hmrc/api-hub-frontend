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

import controllers.actions.IdentifierAction
import controllers.helpers.ErrorResultBuilder
import models.AddAnApiContext.{AddAnApi, AddEndpoints}
import models.requests.IdentifierRequest
import models.{AddAnApiContext, NormalMode, UserAnswers}
import navigation.Navigator
import pages.{AddAnApiApiIdPage, AddAnApiContextPage, AddAnApiSelectApplicationPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def addAnApi(apiId: String): Action[AnyContent] = identify.async {
    implicit request =>
      startJourney(apiId, commonUserAnswers(apiId, AddAnApi))
  }

  def addEndpoints(applicationId: String, apiId: String): Action[AnyContent] = identify.async {
    implicit request =>
      startJourney(
        apiId,
        commonUserAnswers(apiId, AddEndpoints)
          .flatMap(_.set(AddAnApiSelectApplicationPage, applicationId))
      )
  }

  private def commonUserAnswers(apiId: String, context: AddAnApiContext)(implicit request: IdentifierRequest[_]): Try[UserAnswers] = {
    UserAnswers(
      id = request.user.userId,
      lastUpdated = clock.instant()
    )
      .set(AddAnApiApiIdPage, apiId)
      .flatMap(_.set(AddAnApiContextPage, context))
  }

  private def startJourney(id: String, userAnswers: Try[UserAnswers])(implicit request: IdentifierRequest[_]) = {
    apiHubService.getApiDetail(id).flatMap {
      case Some(_) =>
        for {
          userAnswers <- Future.fromTry(userAnswers)
          _           <- addAnApiSessionRepository.set(userAnswers)
        } yield Redirect(navigator.nextPage(AddAnApiApiIdPage, NormalMode, userAnswers))
      case None =>
        Future.successful(
          errorResultBuilder.notFound(
            Messages("site.apiNotFound.heading"),
            Messages("site.apiNotFound.message", id)
          )
        )
    }
  }

}
