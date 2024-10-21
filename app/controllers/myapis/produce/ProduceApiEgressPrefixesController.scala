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

import config.FrontendAppConfig
import controllers.actions.*
import forms.myapis.produce.ProduceApiEgressPrefixesFormProvider
import models.Mode
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEgressPrefixesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiEgressPrefixesController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    produceApiSessionRepository: ProduceApiSessionRepository,
                                                    navigator: Navigator,
                                                    identify: IdentifierAction,
                                                    getData: ProduceApiDataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    formProvider: ProduceApiEgressPrefixesFormProvider,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: ProduceApiEgressPrefixesView,
                                                    config: FrontendAppConfig
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => Ok(view(form, mode, request.user, config.helpDocsPath))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => Redirect(routes.ProduceApiHodController.onPageLoad(mode))
  }
}
