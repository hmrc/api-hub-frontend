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
import models.Mode
import navigation.Navigator
import pages.myapis.produce.ProduceApiEgressPrefixesPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEgressPrefixesView
import forms.myapis.produce.ProduceApiEgressPrefixesFormProvider
import scala.concurrent.Future
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import models.myapis.produce.ProduceApiEgressPrefixes
import play.api.data.Form
import viewmodels.myapis.produce.ProduceApiEgressPrefixesViewModel
import models.requests.DataRequest

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
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiEgressPrefixesPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(buildView(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(buildView(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiEgressPrefixesPage, value))
            _              <- produceApiSessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiEgressPrefixesPage, mode, updatedAnswers))
      )
  }

  private def buildView(form: Form[ProduceApiEgressPrefixes], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiEgressPrefixesViewModel("produceApiEgressPrefix.heading", routes.ProduceApiEgressPrefixesController.onSubmit(mode)), request.user, config.helpDocsPath)
  }

}
