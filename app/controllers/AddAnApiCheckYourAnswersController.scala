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
import models.UserAnswers
import models.api.ApiDetail
import models.application.Application
import pages.{AddAnApiApiIdPage, AddAnApiSelectApplicationPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.ApiHubService
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
  apiHubService: ApiHubService
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        application <- fetchApplication(request.userAnswers)
        applicationSummary <- buildApplicationSummary(application)
        apiDetail <- fetchApiDetail(request.userAnswers)
        apiDetailSummary <- buildApiDetailSummary(apiDetail)
        endpointsSummary <- buildEndpointsSummary(request.userAnswers, apiDetail)
      } yield SummaryListViewModel(
        rows = Seq(applicationSummary, apiDetailSummary, endpointsSummary).flatten
      )).map(summaryList => Ok(view(summaryList, Some(request.user))))
  }

  private def fetchApplication(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Option[Application]] = {
    userAnswers.get(AddAnApiSelectApplicationPage) match {
      case Some(applicationId) => apiHubService.getApplication(applicationId, enrich = false)
      case None => Future.successful(None)
    }
  }

  private def fetchApiDetail(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Option[ApiDetail]] = {
    userAnswers.get(AddAnApiApiIdPage) match {
      case Some(apiId) => apiHubService.getApiDetail(apiId)
      case None => Future.successful(None)
    }
  }

  private def buildApplicationSummary(application: Option[Application])(implicit request: Request[_]): Future[Option[SummaryListRow]] = {
    Future.successful(AddAnApiSelectApplicationSummary.row(application))
  }

  private def buildApiDetailSummary(apiDetail: Option[ApiDetail])(implicit request: Request[_]): Future[Option[SummaryListRow]] = {
    Future.successful(AddAnApiApiIdSummary.row(apiDetail))
  }

  private def buildEndpointsSummary(userAnswers: UserAnswers, apiDetail: Option[ApiDetail])(implicit request: Request[_]): Future[Option[SummaryListRow]] = {
    Future.successful(
      apiDetail.flatMap(AddAnApiSelectEndpointsSummary.row(userAnswers, _))
    )
  }

}
