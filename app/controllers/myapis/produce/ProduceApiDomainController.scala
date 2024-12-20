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

import config.Domains
import controllers.actions.*
import forms.myapis.produce.ProduceApiDomainFormProvider

import javax.inject.Inject
import models.Mode
import models.myapis.produce.ProduceApiDomainSubdomain
import navigation.Navigator
import pages.myapis.produce.ProduceApiDomainPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.data.Form
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiDomainView
import models.requests.DataRequest
import scala.concurrent.{ExecutionContext, Future}
import viewmodels.myapis.produce.ProduceApiDomainViewModel

class ProduceApiDomainController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            sessionRepository: ProduceApiSessionRepository,
                                            navigator: Navigator,
                                            identify: IdentifierAction,
                                            getData: ProduceApiDataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: ProduceApiDomainFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: ProduceApiDomainView,
                                            domains: Domains,
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiDomainPage) match {
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiDomainPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiDomainPage, mode, updatedAnswers))
      )
  }

  private def buildView(form: Form[ProduceApiDomainSubdomain], mode: Mode)(implicit request: DataRequest[AnyContent]) = {
    view(form, ProduceApiDomainViewModel("produceApiDomain.heading", routes.ProduceApiDomainController.onSubmit(mode)), request.user, domains)
  }

}
