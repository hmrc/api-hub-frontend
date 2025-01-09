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

package controllers.myapis.produce

import config.{Domains, Hods}
import controllers.actions.*
import models.{CheckMode, UserAnswers}
import models.api.ApiStatus
import models.deployment.{DeploymentsRequest, DeploymentsResponse, EgressMapping, FailuresResponse, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.myapis.produce.{ProduceApiChooseEgress, ProduceApiDomainSubdomain, ProduceApiEgressPrefixes}
import models.requests.DataRequest
import models.team.Team
import pages.myapis.produce.*
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.myapis.produce.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.myapis.DeploymentSuccessView
import views.html.myapis.produce.{ProduceApiCheckYourAnswersView, ProduceApiDeploymentErrorView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiCheckYourAnswersController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      produceApiSessionRepository: ProduceApiSessionRepository,
                                                      identify: IdentifierAction,
                                                      getData: ProduceApiDataRetrievalAction,
                                                      requireData: DataRequiredAction,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: ProduceApiCheckYourAnswersView,
                                                      successView: DeploymentSuccessView,
                                                      errorView: ProduceApiDeploymentErrorView,
                                                      hods: Hods,
                                                      domains: Domains,
                                                      apiHubService: ApiHubService,
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(view(SummaryListViewModel(summaryListRows(request.userAnswers)), request.user))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate(request).fold(
        call => Future.successful(Redirect(call)), {
        case (apiTitle, deployment) => apiHubService.generateDeployment(deployment).flatMap {
          case response: SuccessfulDeploymentsResponse =>
            produceApiSessionRepository.clear(request.user.userId)
              .map(_ =>
                Redirect(
                  controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onSuccess(
                    apiTitle, response.id
                  )
                )
              )
          case InvalidOasResponse(failure) =>
            Future.successful(BadRequest(errorView(request.user, failure)))
        }})
  }

  def onSuccess(apiName: String, publisherReference: String): Action[AnyContent] = identify {
    implicit request =>
      Ok(successView(request.user, publisherReference, apiName))
  }

  def onCancel(): Action[AnyContent] = identify.async {
    implicit request =>
      produceApiSessionRepository.clear(request.user.userId)
        .map(_ =>
          Redirect(controllers.routes.IndexController.onPageLoad)
        )
  }

  private def validate(request: DataRequest[?]): Either[Call, (String, DeploymentsRequest)] =
    for {
      apiTitle <- validateApiTile(request.userAnswers)
      shortDescription <- validateShortDescription(request.userAnswers)
      egress <- validateEgress(request.userAnswers)
      team <- validateTeam(request.userAnswers)
      oas <- validateOas(request.userAnswers)
      passthrough <- validatePassthrough(request.userAnswers, request.user.permissions.canSupport)
      apiStatus <- validateApiStatus(request.userAnswers)
      domainSubdomain <- validateDomainSubdomain(request.userAnswers)
      hods <- validateHods(request.userAnswers)
      egressPrefixes <- validateEgressPrefixes(request.userAnswers)
      deployment = DeploymentsRequest(
        lineOfBusiness = "apim",
        name = ProduceApiCheckYourAnswersController.formatAsKebabCase(apiTitle),
        description = shortDescription,
        egress = egress,
        teamId = team.id,
        oas = oas,
        passthrough = passthrough,
        status = apiStatus.toString,
        domain = domainSubdomain.domain,
        subDomain = domainSubdomain.subDomain,
        hods = hods.toSeq,
        prefixesToRemove = egressPrefixes.prefixes,
        egressMappings = Some(egressPrefixes.getMappings.map(m =>
          EgressMapping(m.existing, m.replacement)
        )),
      )
    } yield (apiTitle, deployment)

  private def validateApiTile(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ProduceApiEnterApiTitlePage) match {
      case Some(apiTitle) => Right(apiTitle)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateTeam(userAnswers: UserAnswers): Either[Call, Team] = {
    userAnswers.get(ProduceApiChooseTeamPage) match {
      case Some(team) => Right(team)
      case None => Left(routes.ProduceApiChooseTeamController.onPageLoad(CheckMode))
    }
  }

  private def validateOas(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ProduceApiEnterOasPage) match {
      case Some(oas) => Right(oas)
      case None => Left(routes.ProduceApiEnterOasController.onPageLoad(CheckMode))
    }
  }

  private def validateShortDescription(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ProduceApiShortDescriptionPage) match {
      case Some(description) => Right(description)
      case None => Left(routes.ProduceApiShortDescriptionController.onPageLoad(CheckMode))
    }
  }

  private def validateEgress(userAnswers: UserAnswers): Either[Call, String] = {
    (userAnswers.get(ProduceApiEgressAvailabilityPage), userAnswers.get(ProduceApiEgressSelectionPage)) match {
      case (Some(true), Some(egress)) => Right(egress)
      case (Some(true), None) => Left(routes.ProduceApiEgressSelectionController.onPageLoad(CheckMode))
      case (Some(false), _) => Right("null-egress")
      case (None, _) => Left(routes.ProduceApiEgressAvailabilityController.onPageLoad(CheckMode))
    }
  }

  private def validateEgressPrefixes(userAnswers: UserAnswers): Either[Call, ProduceApiEgressPrefixes] = {
    userAnswers.get(ProduceApiEgressPrefixesPage) match {
      case Some(egressPrefixes) => Right(egressPrefixes)
      case None => Left(routes.ProduceApiEgressPrefixesController.onPageLoad(CheckMode))
    }
  }

  private def validateHods(userAnswers: UserAnswers): Either[Call, Set[String]] = {
    userAnswers.get(ProduceApiHodPage) match {
      case Some(hods) => Right(hods)
      case None => Left(routes.ProduceApiHodController.onPageLoad(CheckMode))
    }
  }

  private def validateDomainSubdomain(userAnswers: UserAnswers): Either[Call, ProduceApiDomainSubdomain] = {
    userAnswers.get(ProduceApiDomainPage) match {
      case Some(domainSubdomain) => Right(domainSubdomain)
      case None => Left(routes.ProduceApiDomainController.onPageLoad(CheckMode))
    }
  }

  private def validateApiStatus(userAnswers: UserAnswers): Either[Call, ApiStatus] = {
    userAnswers.get(ProduceApiStatusPage) match {
      case Some(apiStatus) => Right(apiStatus)
      case None => Left(routes.ProduceApiStatusController.onPageLoad(CheckMode))
    }
  }

  private def validatePassthrough(userAnswers: UserAnswers, isSupportUser: Boolean): Either[Call, Boolean] = {
    userAnswers.get(ProduceApiPassthroughPage) match {
      case Some(isPassThrough) => Right(isPassThrough)
      case None if !isSupportUser => Right(false)
      case _ => Left(routes.ProduceApiPassthroughController.onPageLoad(CheckMode))
    }
  }

  private def summaryListRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      ProduceApiChooseTeamSummary.row(userAnswers),
      ProduceApiEnterOasSummary.row(userAnswers),
      ProduceApiNameSummary.row(userAnswers),
      ProduceApiShortDescriptionSummary.row(userAnswers),
      ProduceApiEgressAvailabilitySummary.row(userAnswers),
      ProduceApiEgressSummary.row(userAnswers),
      ProduceApiEgressPrefixesSummary.row(userAnswers),
      ProduceApiHodSummary.row(userAnswers, hods),
      ProduceApiDomainSummary.row(userAnswers, domains),
      ProduceApiSubDomainSummary.row(userAnswers, domains),
      ProduceApiStatusSummary.row(userAnswers),
      ProduceApiPassthroughSummary.row(userAnswers),
    ).flatten
}

object ProduceApiCheckYourAnswersController {
  private[produce] def formatAsKebabCase(text: String): String =
    text.trim
      .toLowerCase()
      .replaceAll("[^\\w\\s_-]", "")
      .split("[\\s_]")
      .filterNot(_.isBlank)
      .mkString("-")

}
