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

import config.FrontendAppConfig
import controllers.actions.*
import forms.myapis.produce.ProduceApiHowToCreateFormProvider
import models.Mode
import models.myapis.produce.ProduceApiHowToCreate
import models.requests.DataRequest
import models.user.UserModel
import navigation.Navigator
import pages.AddAnApiApiPage
import pages.myapis.update.{UpdateApiApiPage, UpdateApiHowToUpdatePage, UpdateApiStartPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UpdateApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.{ProduceApiHowToCreateViewModel, UpdateApiHowToUpdateViewBannerModel}
import views.html.myapis.produce.ProduceApiHowToCreateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiHowToUpdateController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                identify: IdentifierAction,
                                                val controllerComponents: MessagesControllerComponents,
                                                formProvider: ProduceApiHowToCreateFormProvider,
                                                getData: UpdateApiDataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                view: ProduceApiHowToCreateView,
                                                frontendAppConfig: FrontendAppConfig,
                                                sessionRepository: UpdateApiSessionRepository,
                                                navigator: Navigator
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider("myApis.update.howtoupdate.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(UpdateApiHowToUpdatePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      buildView(preparedForm, mode, Ok)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          buildView(formWithErrors, mode, BadRequest),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateApiHowToUpdatePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpdateApiHowToUpdatePage, mode, updatedAnswers))
      )
  }

  private def buildView(form: Form[ProduceApiHowToCreate], mode: Mode, status: Status)(implicit request: DataRequest[AnyContent]) = {
    val viewModel = ProduceApiHowToCreateViewModel(
      "myApis.update.howtoupdate.title",
      "myApis.update.howtoupdate.heading",
      Some(UpdateApiHowToUpdateViewBannerModel("myApis.update.howtoupdate.banner.title", "myApis.update.howtoupdate.banner.content")),
      "update",
      controllers.myapis.update.routes.UpdateApiHowToUpdateController.onSubmit(mode))

    request.userAnswers.get(UpdateApiApiPage) match {
      case Some(apiDetail) => Future.successful(status(view(form, viewModel, request.user)))
      case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
