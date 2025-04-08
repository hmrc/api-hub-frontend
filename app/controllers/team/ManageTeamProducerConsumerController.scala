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

package controllers.team

import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApplicationAuthActionProvider, CreateTeamDataRetrievalAction, DataRequiredAction, IdentifierAction}
import forms.YesNoFormProvider
import models.NormalMode
import models.application.ApplicationLenses.*
import navigation.Navigator
import org.glassfish.jersey.message.internal.FormProvider
import pages.CreateTeamApiProducerConsumerPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.ManageTeamMembersView
import views.html.team.SetTeamApiProducerView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageTeamProducerConsumerController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: CreateTeamDataRetrievalAction,
  requireData: DataRequiredAction,
  view: SetTeamApiProducerView,
  formProvider: YesNoFormProvider,
  navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider("removeApiConfirmation.error")

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      Future.successful(Ok(view(form, navigator.nextPage(CreateTeamApiProducerConsumerPage, NormalMode, request.userAnswers), request.user)))
  }

}
