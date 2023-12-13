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
import forms.AddAnApiSelectEndpointsFormProvider
import models.api.ApiDetail
import models.application.Application
import models.requests.DataRequest
import models.{AddAnApiContext, AvailableEndpoints, Mode}
import navigation.Navigator
import pages.{AddAnApiApiPage, AddAnApiSelectApplicationPage, AddAnApiSelectEndpointsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import repositories.AddAnApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddAnApiSelectEndpointsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddAnApiSelectEndpointsController @Inject()(
  override val messagesApi: MessagesApi,
  sessionRepository: AddAnApiSessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: AddAnApiDataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AddAnApiSelectEndpointsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AddAnApiSelectEndpointsView,
  checkContext: AddAnApiCheckContextActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(
    mode: Mode,
    context: AddAnApiContext
  ): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(context)).async {
    implicit request =>
      (request.userAnswers.get(AddAnApiApiPage), request.userAnswers.get(AddAnApiSelectApplicationPage)) match {
        case (Some(apiDetail), Some(application)) =>
          val form = formProvider.apply(apiDetail, application)

          val preparedForm = request.userAnswers.get(AddAnApiSelectEndpointsPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Future.successful(Ok(view(preparedForm, mode, context, Some(request.user), apiDetail, application)))
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode, context: AddAnApiContext): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(context)).async {
    implicit request =>
      (request.userAnswers.get(AddAnApiApiPage), request.userAnswers.get(AddAnApiSelectApplicationPage)) match {
        case (Some(apiDetail), Some(application)) =>
          val form = formProvider.apply(apiDetail, application)

          form.bindFromRequest(formDataWithExistingEndpoints(request, apiDetail, application)).fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, context, Some(request.user), apiDetail, application))),
            selectedScopes =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnApiSelectEndpointsPage, selectedScopes))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AddAnApiSelectEndpointsPage, mode, updatedAnswers))
          )
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def formDataWithExistingEndpoints(request: DataRequest[AnyContent], apiDetail: ApiDetail, application: Application): Map[String, Seq[String]] = {

    // We are disabling the checkboxes for endpoints that have already been
    // added. Disabled inputs are not submitted with the other form data so we
    // lose the data associated with them when this is posted back to us.
    // So we need to add the already-added endpoints into the form data.

    request.request.body.asFormUrlEncoded.getOrElse(Map.empty) ++
      AvailableEndpoints(apiDetail, application)
        .toSeq
        .zipWithIndex
        .filter(_._1._2.exists(_.added))
        .map(etc => s"value[${etc._2}]" -> Seq(etc._1._1.toString()))
        .toMap

  }

}
