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

import config.FrontendAppConfig
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ApplicationApiBuilder
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.application.accessrequest.{RequestProductionAccessApisPage, RequestProductionAccessApplicationPage, RequestProductionAccessStartPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.AccessRequestSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RequestProductionAccessStartController @Inject()(
    override val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    accessRequestSessionRepository: AccessRequestSessionRepository,
    clock: Clock,
    applicationAuth: ApplicationAuthActionProvider,
    appConfig: FrontendAppConfig,
    applicationApiBuilder: ApplicationApiBuilder,
    navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen applicationAuth(id)).async {
    implicit request =>
      for {
        applicationApis <- applicationApiBuilder.build(request.application)
        userAnswers <- Future.fromTry(
          UserAnswers(
            id = request.identifierRequest.user.userId,
            lastUpdated = clock.instant()
          )
          .set(RequestProductionAccessApplicationPage, request.application)
            .flatMap(_.set(RequestProductionAccessApisPage, applicationApis))
        )
        _ <- accessRequestSessionRepository.set(userAnswers)
      } yield Redirect(navigator.nextPage(RequestProductionAccessStartPage, NormalMode, userAnswers))
  }

}
