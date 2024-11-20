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

package controllers.myapis.update

import config.Domains
import controllers.actions.*
import controllers.myapis.update.routes
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import pages.myapis.update.UpdateApiDomainPage
import views.html.myapis.produce.ProduceApiDomainView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import forms.myapis.produce.ProduceApiDomainFormProvider
import models.myapis.produce.ProduceApiDomainSubdomain
import viewmodels.myapis.produce.ProduceApiDomainViewModel
import models.requests.DataRequest
import repositories.UpdateApiSessionRepository
import navigation.Navigator

class UpdateApiDomainController @Inject()(
                               override val messagesApi: MessagesApi,
                               sessionRepository: UpdateApiSessionRepository,
                               navigator: Navigator,
                               identify: IdentifierAction,
                               getData: UpdateApiDataRetrievalAction,
                               requireData: DataRequiredAction,
                               formProvider: ProduceApiDomainFormProvider,
                               val controllerComponents: MessagesControllerComponents,
                               view: ProduceApiDomainView,
                               domains: Domains,
                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(UpdateApiDomainPage) match {
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateApiDomainPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiDomainPage, mode, updatedAnswers))
      )
  }

  private def buildView(form: Form[ProduceApiDomainSubdomain], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiDomainViewModel("updateApiDomain.heading", routes.UpdateApiDomainController.onSubmit(mode)), request.user, domains)
  }
}
