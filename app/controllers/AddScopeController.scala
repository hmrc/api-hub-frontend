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
import models.application.{EnvironmentName, NewScope}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.html.components.FormWithCSRF
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
                                    getData: DataRetrievalAction,
                                    formProvider: ScopeNameFormProvider,
                                    formWithCSRF: FormWithCSRF
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = identify.async {
    implicit request =>
      apiHubService.getApplication(id) map {
        case Some(application) => Ok(view(application.id, form))
        case _ => NotFound
      }
  }

  case class ScopeData(scopeName: String, dev: Option[String], test: Option[String], preProd: Option[String], prod: Option[String])

  val scopeForm = Form(
    mapping(
      "scope-name" -> text,
      "dev" -> optional(text),
      "test" -> optional(text),
      "preProd" -> optional(text),
      "prod" -> optional(text)
    )(ScopeData.apply)(ScopeData.unapply)
  )

  def onSubmit(id: String): Action[AnyContent] = identify.async {
    implicit request =>
      val scopeData = scopeForm.bindFromRequest.get
      val envs = Seq(scopeData.dev, scopeData.test, scopeData.preProd, scopeData.prod).flatten[String].flatMap(s => EnvironmentName.enumerable.withName(s))

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(id, formWithErrors))),

        value => {
          val value1 = NewScope(value, envs)
          apiHubService.requestAdditionalScope(id, value1).map(
            application => Redirect(routes.RequestScopeSuccessController.onPageLoad(id)))
        })
  }
}
