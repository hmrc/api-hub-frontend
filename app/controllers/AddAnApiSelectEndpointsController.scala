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
import controllers.helpers.ErrorResultBuilder
import forms.AddAnApiSelectEndpointsFormProvider
import models.Mode
import navigation.Navigator
import pages.{AddAnApiApiIdPage, AddAnApiSelectEndpointsPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
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
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(AddAnApiApiIdPage) match {
        case Some(apiId) =>
          apiHubService.getApiDetail(apiId).flatMap {
            case Some(apiDetail) =>
              val form = formProvider.apply(apiDetail)

              val preparedForm = request.userAnswers.get(AddAnApiSelectEndpointsPage) match {
                case None => form
                case Some(value) => form.fill(value)
              }

              Future.successful(Ok(view(preparedForm, mode, Some(request.user), apiDetail)))
            case None => apiNotFound(apiId)
          }
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(AddAnApiApiIdPage) match {
        case Some(apiId) =>
          apiHubService.getApiDetail(apiId).flatMap {
            case Some(apiDetail) =>
              val form = formProvider.apply(apiDetail)

              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, mode, Some(request.user), apiDetail))),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnApiSelectEndpointsPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(AddAnApiSelectEndpointsPage, mode, updatedAnswers))
              )
            case None => apiNotFound(apiId)
          }
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def apiNotFound(apiId: String)(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.notFound(
        Messages("site.apiNotFound.heading"),
        Messages("site.apiNotFound.message", apiId)
      )
    )
  }

}
