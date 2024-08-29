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
import models.api.ApiDetail
import models.application.Application
import models.{AddAnApiContext, UserAnswers}
import pages.{AddAnApiApiPage, AddAnApiSelectApplicationPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{AddAnApiApiIdSummary, AddAnApiSelectApplicationSummary, AddAnApiSelectEndpointsSummary}
import viewmodels.govuk.summarylist._
import views.html.AddAnApiCheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddAnApiCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: AddAnApiDataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: AddAnApiCheckYourAnswersView,
  checkContext: AddAnApiCheckContextActionProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(context: AddAnApiContext): Action[AnyContent] = (identify andThen getData andThen requireData andThen checkContext(context)).async {
    implicit request =>
      (for {
        application <- fetchApplication(request.userAnswers)
        applicationSummary <- buildApplicationSummary(application, context)
        apiDetail <- fetchApiDetail(request.userAnswers)
        apiDetailSummary <- buildApiDetailSummary(apiDetail)
        endpointsSummary <- buildEndpointsSummary(request.userAnswers, apiDetail, application, context)
      } yield SummaryListViewModel(
        rows = Seq(applicationSummary, apiDetailSummary, endpointsSummary).flatten
      )).map(summaryList => Ok(view(summaryList, Some(request.user), context)))
  }

  private def fetchApplication(userAnswers: UserAnswers): Future[Option[Application]] = {
    userAnswers.get(AddAnApiSelectApplicationPage) match {
      case Some(application) => Future.successful(Some(application))
      case None => Future.successful(None)
    }
  }

  private def fetchApiDetail(userAnswers: UserAnswers): Future[Option[ApiDetail]] = {
    userAnswers.get(AddAnApiApiPage) match {
      case Some(apiDetail) => Future.successful(Some(apiDetail))
      case None => Future.successful(None)
    }
  }

  private def buildApplicationSummary(
    application: Option[Application],
    context: AddAnApiContext
  )(implicit request: Request[?]): Future[Option[SummaryListRow]] = {
    Future.successful(AddAnApiSelectApplicationSummary.row(application, context))
  }

  private def buildApiDetailSummary(apiDetail: Option[ApiDetail])(implicit request: Request[?]): Future[Option[SummaryListRow]] = {
    Future.successful(AddAnApiApiIdSummary.row(apiDetail))
  }

  private def buildEndpointsSummary(
    userAnswers: UserAnswers,
    apiDetail: Option[ApiDetail],
    application: Option[Application],
    context: AddAnApiContext
  )(implicit request: Request[?]): Future[Option[SummaryListRow]] = {
    Future.successful(
      (apiDetail, application) match {
        case (Some(api), Some(app)) => AddAnApiSelectEndpointsSummary.row(userAnswers, api, app, context)
        case _ => None
      }
    )
  }

}
