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
import forms.YesNoFormProvider
import models.api.ApiStatus
import models.deployment.{DeploymentsRequest, EgressMapping, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.myapis.produce.{ProduceApiDomainSubdomain, ProduceApiEgressPrefixes}
import models.requests.DataRequest
import models.team.Team
import models.{CheckMode, UserAnswers}
import pages.myapis.produce.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.myapis.produce.*
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.myapis.DeploymentSuccessViewModel
import viewmodels.myapis.produce.{ProduceApiCheckYourAnswersViewModel, ProduceApiDeploymentErrorViewModel}
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
                                                      formProvider: YesNoFormProvider,
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private lazy val failureViewModel = ProduceApiDeploymentErrorViewModel(
    controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onCancel(),
    controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad()
  )
  private val form = formProvider("produceApiCheckYourAnswers.noEgress.confirmation.error")

  private def hasEgressSelected(userAnswers: UserAnswers) =
    userAnswers.get(ProduceApiSelectEgressPage).exists(!_.isBlank)

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(buildView(form))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateNoEgressAcknowledgement(request).fold(
        formWithErrors => Future.successful(BadRequest(buildView(formWithErrors))),
        _ => validate(request).fold(
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
              Future.successful(BadRequest(errorView(request.user, failure, failureViewModel)))
          }})
      )
  }

  def onSuccess(apiName: String, publisherReference: String): Action[AnyContent] = identify {
    implicit request =>
      Ok(successView(DeploymentSuccessViewModel(
        request.user, 
        publisherReference, 
        apiName, 
        "api.deployment.success.feedback.create.heading", 
        "api.deployment.success.feedback.create.message"
      )))
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
      addPrefixes <- validateAddPrefixes(request.userAnswers)
      egressPrefixes <- validateEgressPrefixes(request.userAnswers, addPrefixes)
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
        prefixesToRemove = egressPrefixes.map(_.prefixes).getOrElse(Seq.empty),
        egressMappings = egressPrefixes.map(_.getMappings.map(m =>
          EgressMapping(m.existing, m.replacement)
        )),
      )
    } yield (apiTitle, deployment)

  private def validateNoEgressAcknowledgement(request: DataRequest[?]): Either[Form[?], Unit] =
    if(hasEgressSelected(request.userAnswers))
      Right(())
    else
      form.bindFromRequest()(request)
        .fold(Left(_),_ => Right(()))

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

  private def validateEgress(userAnswers: UserAnswers): Either[Call, Option[String]] = {
    userAnswers.get(ProduceApiSelectEgressPage) match {
      case Some(egress) => Right(Option.when(!egress.isBlank)(egress))
      case None => Left(routes.ProduceApiSelectEgressController.onPageLoad(CheckMode))
    }
  }

  private def validateAddPrefixes(userAnswers: UserAnswers): Either[Call, Boolean] = {
    userAnswers.get(ProduceApiAddPrefixesPage) match
      case Some(addPrefixes) => Right(addPrefixes)
      case None => Left(routes.ProduceApiAddPrefixesController.onPageLoad(CheckMode))
  }

  private def validateEgressPrefixes(userAnswers: UserAnswers, addPrefixes: Boolean): Either[Call, Option[ProduceApiEgressPrefixes]] = {
    if (addPrefixes) {
      userAnswers.get(ProduceApiEgressPrefixesPage) match {
        case Some(egressPrefixes) => Right(Some(egressPrefixes))
        case None => Left(routes.ProduceApiEgressPrefixesController.onPageLoad(CheckMode))
      }
    }
    else {
      Right(None)
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

  private def buildView(form: Form[?])(implicit request: DataRequest[AnyContent]) = {
    view(
      SummaryListViewModel(summaryListRows(request.userAnswers)),
      request.user,
      ProduceApiCheckYourAnswersViewModel(
        controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onSubmit(),
      ),
      maybeForm = Option.when(!hasEgressSelected(request.userAnswers))(form)
    )
  }
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
