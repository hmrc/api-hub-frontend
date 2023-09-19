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

import controllers.actions.{AddAnApiDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.AddAnApiSelectApplicationFormProvider
import models.Mode
import models.api.ApiDetail
import models.api.ApiDetailLenses.ApiDetailLensOps
import models.application.Application
import models.application.ApplicationLenses.ApplicationLensOps
import navigation.Navigator
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
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
  view: AddAnApiSelectApplicationView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport{

  private val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.user.email.fold(noEmail())(
        email =>
          request.userAnswers.get(AddAnApiApiIdPage) match {
            case Some(apiId) =>
              apiHubService.getApiDetail(apiId).flatMap {
                case Some(apiDetail) =>
                  apiHubService.getUserApplications(email, enrich = true).map {
                    applications =>
                      Ok(
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
                case None => apiNotFound()
              }
            case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(AddAnApiSelectApplicationPage, mode, request.userAnswers))
  }

  private def noEmail()(implicit request: Request[_]): Future[Result] = {
    Future.successful(
      errorResultBuilder.internalServerError("The current user does not have an email address")
    )
  }

  private def apiNotFound()(implicit request: Request[_]): Future[Result] = {
    Future.successful(NotFound)
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
