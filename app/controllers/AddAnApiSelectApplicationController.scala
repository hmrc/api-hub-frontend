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

import controllers.actions.{AddAnApiCheckContextActionProvider, AddAnApiDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.AddAnApiSelectApplicationFormProvider
import models.api.ApiDetail
import models.api.ApiDetailLenses.ApiDetailLensOps
import models.application.Application
import models.application.ApplicationLenses.ApplicationLensOps
import models.requests.DataRequest
import models.{AddAnApi, Mode}
import navigation.Navigator
import pages.{AddAnApiApiPage, AddAnApiSelectApplicationPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import repositories.AddAnApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddAnApiSelectApplicationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddAnApiSelectApplicationController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  override val messagesApi: MessagesApi,
  sessionRepository: AddAnApiSessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: AddAnApiDataRetrievalAction,
  requireData: DataRequiredAction,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  formProvider: AddAnApiSelectApplicationFormProvider,
  view: AddAnApiSelectApplicationView,
  checkContext: AddAnApiCheckContextActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport{

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(AddAnApi)).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(AddAnApiSelectApplicationPage) match {
        case None => form
        case Some(application) => form.fill(application.id)
      }

      buildView(mode, preparedForm, Ok)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(AddAnApi)).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          buildView(mode, formWithErrors, BadRequest),
        applicationId =>
          apiHubService.getApplication(applicationId, false).flatMap {
            case Some(application) =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnApiSelectApplicationPage, application))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AddAnApiSelectApplicationPage, mode, updatedAnswers))
            case _ => applicationNotFound(applicationId)
          }
      )
  }

  private def buildView(mode: Mode, form: Form[String], status: Status)(implicit request: DataRequest[AnyContent]) = {
    request.user.email.fold(noEmail())(
      email =>
        request.userAnswers.get(AddAnApiApiPage) match {
          case Some(apiDetail) =>
            apiHubService.getUserApplications(email, enrich = true).map {
              applications =>
                status(
                  view(
                    form,
                    mode,
                    Some(request.user),
                    apiDetail,
                    applicationsWithAccess(apiDetail, applications),
                    applicationsWithoutAccess(apiDetail, applications)
                  )
                )
            }
          case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    )
  }

  private def noEmail()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.internalServerError("The current user does not have an email address")
    )
  }

  private def applicationNotFound(applicationId: String)(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.notFound(
        heading = Messages("site.applicationNotFoundHeading"),
        message = Messages("site.applicationNotFoundMessage", applicationId)
      )
    )
  }

  private def applicationsWithAccess(apiDetail: ApiDetail, applications: Seq[Application]): Seq[Application] = {
    applications.filter(
      application =>
        apiDetail.getRequiredScopeNames.intersect(application.getRequiredScopeNames).nonEmpty
    )
  }

  private def applicationsWithoutAccess(apiDetail: ApiDetail, applications: Seq[Application]): Seq[Application] = {
    applications.filter(
      application =>
        apiDetail.getRequiredScopeNames.intersect(application.getRequiredScopeNames).isEmpty
    )
  }

}
