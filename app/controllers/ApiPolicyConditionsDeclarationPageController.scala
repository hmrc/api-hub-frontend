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
import forms.ApiPolicyConditionsDeclarationPageFormProvider
import models.{AddAnApiContext, ApiPolicyConditionsDeclaration, Mode}
import models.requests.DataRequest
import navigation.Navigator
import pages.{AddAnApiApiIdPage, ApiPolicyConditionsDeclarationPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApiPolicyConditionsDeclarationPageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ApiPolicyConditionsDeclarationPageController @Inject()(
  override val messagesApi: MessagesApi,
  sessionRepository: AddAnApiSessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: AddAnApiDataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ApiPolicyConditionsDeclarationPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ApiPolicyConditionsDeclarationPageView,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  checkContext: AddAnApiCheckContextActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(
    mode: Mode,
    context: AddAnApiContext
  ): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(context)).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(ApiPolicyConditionsDeclarationPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      buildView(mode, context, preparedForm, Ok)
  }

  def onSubmit(
    mode: Mode,
    context: AddAnApiContext
  ): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(context)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          buildView(mode, context, formWithErrors, BadRequest),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ApiPolicyConditionsDeclarationPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ApiPolicyConditionsDeclarationPage, mode, updatedAnswers))
      )
  }

  private def buildView(
    mode: Mode,
    context: AddAnApiContext,
    form: Form[Set[ApiPolicyConditionsDeclaration]],
    status: Status
  )(implicit request: DataRequest[AnyContent]):Future[Result] = {
    request.userAnswers.get(AddAnApiApiIdPage) match {
      case Some(apiId) =>
        apiHubService.getApiDetail(apiId).flatMap {
          case Some(apiDetail) =>
            Future.successful(status(
              view(
                form,
                mode,
                context,
                apiDetail
              )
            ))

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
