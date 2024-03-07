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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{OasEditorsAceView, OasEditorsCodeMirrorView, OasEditorsMonacoView, OasEditorsSummaryView, OasEditorsSwaggerNewView, OasEditorsSwaggerOldView}

import javax.inject.Inject

class OasEditorsController @Inject()(
                                      val controllerComponents: MessagesControllerComponents,
                                      summaryView: OasEditorsSummaryView,
                                      swaggerOldView: OasEditorsSwaggerOldView,
                                      swaggerNewView: OasEditorsSwaggerNewView,
                                      monacoView: OasEditorsMonacoView,
                                      aceView: OasEditorsAceView,
                                      codeMirrorView: OasEditorsCodeMirrorView,
                                      ) extends FrontendBaseController with I18nSupport {

  def loadSummary: Action[AnyContent] = Action { implicit request =>
    Ok(summaryView())
  }
  def loadSwaggerOld: Action[AnyContent] = Action { implicit request =>
    Ok(swaggerOldView())
  }
  def loadSwaggerNew: Action[AnyContent] = Action { implicit request =>
    Ok(swaggerNewView())
  }
  def loadAce: Action[AnyContent] = Action { implicit request =>
    Ok(aceView())
  }
  def loadMonaco: Action[AnyContent] = Action { implicit request =>
    Ok(monacoView())
  }
  def loadCodeMirror: Action[AnyContent] = Action { implicit request =>
    Ok(codeMirrorView())
  }
}
