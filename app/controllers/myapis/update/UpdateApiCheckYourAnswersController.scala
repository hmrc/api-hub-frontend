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

import config.{Domains, Hods}
import controllers.actions.*
import controllers.helpers.ErrorResultBuilder
import forms.YesNoFormProvider
import models.api.{ApiDetail, ApiStatus}
import models.deployment.*
import models.myapis.produce.{ProduceApiChooseEgress, ProduceApiDomainSubdomain, ProduceApiEgressPrefixes}
import models.requests.DataRequest
import models.team.Team
import models.user.UserModel
import models.{CheckMode, UserAnswers}
import pages.myapis.update.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.UpdateApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.myapis.update.*
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.myapis.DeploymentSuccessViewModel
import viewmodels.myapis.produce.{ProduceApiCheckYourAnswersViewModel, ProduceApiDeploymentErrorViewModel}
import views.html.myapis.DeploymentSuccessView
import views.html.myapis.produce.{ProduceApiCheckYourAnswersView, ProduceApiDeploymentErrorView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateApiCheckYourAnswersController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      updateApiSessionRepository: UpdateApiSessionRepository,
                                                      identify: IdentifierAction,
                                                      getData: UpdateApiDataRetrievalAction,
                                                      requireData: DataRequiredAction,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: ProduceApiCheckYourAnswersView,
                                                      successView: DeploymentSuccessView,
                                                      errorView: ProduceApiDeploymentErrorView,
                                                      hods: Hods,
                                                      domains: Domains,
                                                      apiHubService: ApiHubService,
                                                      errorResultBuilder: ErrorResultBuilder,
                                                      formProvider: YesNoFormProvider,
                                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  private lazy val errorViewModel = ProduceApiDeploymentErrorViewModel(
    controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onCancel(),
    controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onPageLoad()
  )
  private val form = formProvider("produceApiCheckYourAnswers.noEgress.confirmation.error")

  private def hasEgressSelected(userAnswers: UserAnswers) =
    userAnswers.get(UpdateApiEgressSelectionPage).isDefined

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(buildView(form))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateNoEgressAcknowledgement(request).fold(
        formWithErrors => Future.successful(BadRequest(buildView(formWithErrors))),
        _ => validate(request).fold(
          call => Future.successful(Redirect(call)),
          { case (apiTitle, publisherReference, redeployment) =>
            apiHubService.updateDeployment(publisherReference, redeployment).flatMap {
              case Some(response: SuccessfulDeploymentsResponse) =>
                updateApiSessionRepository.clear(request.user.userId)
                  .map(_ =>
                    Redirect(
                      controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onSuccess(
                        apiTitle, publisherReference
                      )
                    )
                  )
              case Some(InvalidOasResponse(failure)) =>
                Future.successful(BadRequest(errorView(request.user, failure, errorViewModel)))
              case None =>
                Future.successful(errorResultBuilder.apiNotFound(publisherReference))
            }
          }
        )
      )
  }

  def onSuccess(apiName: String, publisherReference: String): Action[AnyContent] = identify {
    implicit request =>
      Ok(successView(DeploymentSuccessViewModel(
        request.user,
        publisherReference,
        apiName,
        "api.deployment.success.feedback.update.heading",
        "api.deployment.success.feedback.update.message"
      )))
  }

  def onCancel(): Action[AnyContent] = identify.async {
    implicit request =>
      updateApiSessionRepository.clear(request.user.userId)
        .map(_ =>
          Redirect(controllers.routes.IndexController.onPageLoad)
        )
  }

  private def validate(request: DataRequest[?]): Either[Call, (String, String, RedeploymentRequest)] =
    for {
      apiDetail <- validateApiDetail(request.userAnswers)
      shortDescription <- validateShortDescription(request.userAnswers)
      egress <- validateEgress(request.userAnswers)
      oas <- validateOas(request.userAnswers)
      apiStatus <- validateApiStatus(request.userAnswers)
      domainSubdomain <- validateDomainSubdomain(request.userAnswers)
      hods <- validateHods(request.userAnswers)
      addPrefixes <- validateAddPrefixes(request.userAnswers)
      egressPrefixes <- validateEgressPrefixes(request.userAnswers, addPrefixes)
      redeployment = RedeploymentRequest(
        description = shortDescription,
        egress = egress,
        oas = oas,
        status = apiStatus.toString,
        domain = domainSubdomain.domain,
        subDomain = domainSubdomain.subDomain,
        hods = hods.toSeq,
        prefixesToRemove = egressPrefixes.map(_.prefixes).getOrElse(Seq.empty),
        egressMappings = egressPrefixes.map(_.getMappings.map(m =>
          EgressMapping(m.existing, m.replacement)
        )),
      )
    } yield (apiDetail.title, apiDetail.publisherReference, redeployment)

  private def validateNoEgressAcknowledgement(request: DataRequest[?]): Either[Form[?], Unit] =
    if(hasEgressSelected(request.userAnswers))
      Right(())
    else
      form.bindFromRequest()(request)
        .fold(Left(_),_ => Right(()))

  private def validateApiDetail(userAnswers: UserAnswers): Either[Call, ApiDetail] = {
    userAnswers.get(UpdateApiApiPage) match {
      case Some(apiDetail) => Right(apiDetail)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def validateOas(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(UpdateApiEnterOasPage) match {
      case Some(oas) => Right(oas)
      case None => Left(routes.UpdateApiEnterOasController.onPageLoad(CheckMode))
    }
  }

  private def validateShortDescription(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(UpdateApiShortDescriptionPage) match {
      case Some(description) => Right(description)
      case None => Left(routes.UpdateApiShortDescriptionController.onPageLoad(CheckMode))
    }
  }

  private def validateAddPrefixes(userAnswers: UserAnswers): Either[Call, Boolean] = {
    userAnswers.get(UpdateApiAddPrefixesPage) match
      case Some(addPrefixes) => Right(addPrefixes)
      case None => Left(routes.UpdateApiAddPrefixesController.onPageLoad(CheckMode))
  }
  
  private def validateEgress(userAnswers: UserAnswers): Either[Call, Option[String]] = {
    (userAnswers.get(UpdateApiEgressAvailabilityPage), userAnswers.get(UpdateApiEgressSelectionPage)) match {
      case (Some(true), Some(egress)) => Right(Some(egress))
      case (Some(true), None) => Left(routes.UpdateApiEgressSelectionController.onPageLoad(CheckMode))
      case (Some(false), _) => Right(None)
      case (None, _) => Left(routes.UpdateApiEgressAvailabilityController.onPageLoad(CheckMode))
    }
  }

  private def validateEgressPrefixes(userAnswers: UserAnswers, addPrefixes: Boolean): Either[Call, Option[ProduceApiEgressPrefixes]] = {
    if (addPrefixes) {
      userAnswers.get(UpdateApiEgressPrefixesPage) match {
        case Some(egressPrefixes) => Right(Some(egressPrefixes))
        case None => Left(routes.UpdateApiEgressPrefixesController.onPageLoad(CheckMode))
      }
    }
    else {
      Right(None)
    }
  }

  private def validateHods(userAnswers: UserAnswers): Either[Call, Set[String]] = {
    userAnswers.get(UpdateApiHodPage) match {
      case Some(hods) => Right(hods)
      case None => Left(routes.UpdateApiHodController.onPageLoad(CheckMode))
    }
  }

  private def validateDomainSubdomain(userAnswers: UserAnswers): Either[Call, ProduceApiDomainSubdomain] = {
    userAnswers.get(UpdateApiDomainPage) match {
      case Some(domainSubdomain) => Right(domainSubdomain)
      case None => Left(routes.UpdateApiDomainController.onPageLoad(CheckMode))
    }
  }

  private def validateApiStatus(userAnswers: UserAnswers): Either[Call, ApiStatus] = {
    userAnswers.get(UpdateApiStatusPage) match {
      case Some(apiStatus) => Right(apiStatus)
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def summaryListRows(userAnswers: UserAnswers, userModel: UserModel)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      UpdateApiEnterOasSummary.row(userAnswers),
      UpdateApiNameSummary.row(userAnswers),
      UpdateApiShortDescriptionSummary.row(userAnswers),
      UpdateApiEgressAvailabilitySummary.row(userAnswers),
      UpdateApiEgressSummary.row(userAnswers),
      UpdateApiEgressPrefixesSummary.row(userAnswers),
      UpdateApiHodSummary.row(userAnswers, hods),
      UpdateApiDomainSummary.row(userAnswers, domains),
      UpdateApiSubDomainSummary.row(userAnswers, domains),
      UpdateApiStatusSummary.row(userAnswers, userModel),
    ).flatten
  
  private def buildView(form: Form[?])(implicit request: DataRequest[AnyContent]) = {
    view(
      SummaryListViewModel(summaryListRows(request.userAnswers, request.user)),
      request.user,
      ProduceApiCheckYourAnswersViewModel(
        controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onSubmit(),
      ),
      maybeForm = Option.when(!hasEgressSelected(request.userAnswers))(form)
    )
  }
}
