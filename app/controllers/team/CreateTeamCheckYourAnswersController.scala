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

package controllers.team

import com.google.inject.Inject
import controllers.actions.{CreateTeamDataRetrievalAction, DataRequiredAction, IdentifierAction}
import forms.YesNoFormProvider
import models.application.TeamMember
import models.exception.TeamNameNotUniqueException
import models.{CheckMode, UserAnswers}
import models.requests.DataRequest
import models.team.{NewTeam, Team, TeamType}
import pages.{CreateTeamApiProducerConsumerPage, CreateTeamMembersPage, CreateTeamNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.inject.bind
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{CreateTeamAddTeamMemberSummary, CreateTeamApiProducerConsumerSummary, CreateTeamNameSummary}
import viewmodels.govuk.summarylist.*
import views.html.team.CreateTeamCheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CreateTeamCheckYourAnswersController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      identify: IdentifierAction,
                                                      getData: CreateTeamDataRetrievalAction,
                                                      requireData: DataRequiredAction,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: CreateTeamCheckYourAnswersView,
                                                      apiHubService: ApiHubService,
                                                      sessionRepository: CreateTeamSessionRepository,
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private lazy val form = YesNoFormProvider()("createTeamCheckYourAnswers.producingAPIs.confirmation.error")

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => Ok(buildView())
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validate().fold(
        result => Future.successful(result),
        newTeam =>
          apiHubService.createTeam(newTeam).flatMap {
            case Right(team) =>
              for {
                _ <- sessionRepository.clear(request.userAnswers.id)
              } yield Redirect(controllers.team.routes.TeamCreatedController.onPageLoad(team.id))
            case Left(_: TeamNameNotUniqueException) =>
              Future.successful(Redirect(controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode)))
            case Left(e) => throw e
          }
      )
  }

  private def validate()(implicit request: DataRequest[?]): Either[Result, NewTeam] =
    if(isProducerTeam(request.userAnswers))
      form.bindFromRequest()(request)
        .fold(
          formWithErrors => Left(BadRequest(buildView(formWithErrors))),
          _ => validateAnswers(request.userAnswers)
        )
    else
      validateAnswers(request.userAnswers)

  private def validateAnswers(userAnswers: UserAnswers): Either[Result, NewTeam] =
    for {
      name <- validateTeamName(userAnswers)
      teamMembers <- validateTeamMembers(userAnswers)
      teamType <- validateTeamType(userAnswers)
    } yield NewTeam(name, teamMembers, teamType)

  private def validateTeamName(userAnswers: UserAnswers): Either[Result, String] =
    userAnswers.get(CreateTeamNamePage) match {
      case Some(name) => Right(name)
      case _ => Left(Redirect(controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode)))
    }

  private def validateTeamMembers(userAnswers: UserAnswers): Either[Result, Seq[TeamMember]] =
    userAnswers.get(CreateTeamMembersPage) match {
      case Some(teamMembers) => Right(teamMembers)
      case _ => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def validateTeamType(userAnswers: UserAnswers): Either[Result, TeamType] =
    userAnswers.get(CreateTeamApiProducerConsumerPage) match {
      case Some(isProducerTeam) =>
        val teamType = if isProducerTeam then TeamType.ProducerTeam else TeamType.ConsumerTeam
        Right(teamType)
      case _ => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def isProducerTeam(userAnswers: UserAnswers): Boolean =
    userAnswers.get(CreateTeamApiProducerConsumerPage).exists(identity)

  private def buildView(form: Form[?] = form)(implicit request: DataRequest[?]) = {
    val teamName = SummaryListViewModel(
      rows = Seq(
        CreateTeamNameSummary.row(request.userAnswers),
        CreateTeamApiProducerConsumerSummary.row(request.userAnswers),
      ).flatten
    )

    val teamMemberDetails = CreateTeamAddTeamMemberSummary.summary(request.userAnswers)
    val isProducer = request.userAnswers.get(CreateTeamApiProducerConsumerPage).exists(identity)
    view(teamName, teamMemberDetails, Some(request.user), isProducer, form)
  }

}
