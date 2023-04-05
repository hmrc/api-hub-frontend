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

import config.FrontendAppConfig
import controllers.actions._
import forms.NewScopeFormProvider
import models.application.{EnvironmentName, NewScope}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddScopeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddScopeController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    identify: IdentifierAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: AddScopeView,
                                    apiHubService: ApiHubService,
                                    formProvider: NewScopeFormProvider,
                                    config: FrontendAppConfig
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = identify.async {
    implicit request =>
      apiHubService.getApplication(id) map {
        case Some(application) => Ok(view(application.id, form, Some(request.user), config))
        case _ => NotFound
      }
  }

  def onSubmit(id: String): Action[AnyContent] = identify.async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(id, formWithErrors, Some(request.user), config))),

        scopeData => {
          val envs = Seq(scopeData.primary, scopeData.secondary).flatten[String].flatMap(s => EnvironmentName.enumerable.withName(s))
          apiHubService.requestAdditionalScope(id, NewScope(scopeData.scopeName, envs)).map(
            _ => Redirect(routes.RequestScopeSuccessController.onPageLoad(id)))
        })
  }
}

case class ScopeData(scopeName: String, primary: Option[String] = None, secondary: Option[String] = None)
