/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.myapis.produce

import com.google.inject.{Inject, Singleton}
import controllers.actions.IdentifierAction
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.myapis.produce.ProduceApiStartPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.Clock
import scala.concurrent.ExecutionContext

@Singleton
class ProduceApiStartController @Inject() (
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  sessionRepository: ProduceApiSessionRepository,
  clock: Clock,
  navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def startProduceApi(): Action[AnyContent] = identify.async {
    implicit request => {
      val userAnswers = UserAnswers(
        id = request.user.userId,
        lastUpdated = clock.instant()
      )
      sessionRepository.set(userAnswers).map(_ => Redirect(navigator.nextPage(ProduceApiStartPage, NormalMode, userAnswers)))
    }
  }

}
