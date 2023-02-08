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

import controllers.actions._
import forms.ScopeNameFormProvider
import models.Mode
import navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{AddScopeView, ApplicationDetailsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddScopeController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    identify: IdentifierAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: AddScopeView,
                                    apiHubService: ApiHubService,
                                    getData: DataRetrievalAction,
                                    formProvider: ScopeNameFormProvider,
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = identify.async {
    implicit request =>
      apiHubService.getApplication(id) map {
        case Some(application) => Ok(view(application.id, form))
        case _ => NotFound
      }
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(id, formWithErrors))),

        value => {
          // Call add scope api, and then
          Future.successful(Redirect(routes.RequestScopeSuccessController.onPageLoad(id)))

        }
      )

  }

}
