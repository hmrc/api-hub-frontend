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

import cats.data.OptionT
import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.api.{ApiDetail, ApiStatus}
import models.deployment.DeploymentDetails
import models.myapis.produce.{ProduceApiDomainSubdomain, ProduceApiEgressPrefixMapping, ProduceApiEgressPrefixes}
import models.requests.ApiRequest
import models.team.Team
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.{Page, QuestionPage}
import pages.myapis.update.*
import play.api.i18n.I18nSupport
import play.api.libs.json.Writes
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.UpdateApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateApiStartController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  sessionRepository: UpdateApiSessionRepository,
  clock: Clock,
  navigator: Navigator,
  apiHubService: ApiHubService,
  errorResultBuilder: ErrorResultBuilder,
  apiAuth: ApiAuthActionProvider,
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def startProduceApi(id: String): Action[AnyContent] = (identify  andThen apiAuth(id)).async {
    implicit request => {
      if (request.apiDetails.isHubMaintainable) {
        val userAnswers = UserAnswers(
          id = request.identifierRequest.user.userId,
          lastUpdated = clock.instant()
        )

        apiHubService.getDeploymentDetails(request.apiDetails.publisherReference).flatMap(_.fold(
          Future.successful(errorResultBuilder.apiNotFoundInApim(request.apiDetails))
        )(deploymentDetails =>
          for {
            updatedAnswers <- updateAnswer(userAnswers, UpdateApiApiPage, request.apiDetails)
            updatedAnswers <- updateTeamAnswer(updatedAnswers)
            updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiShortDescriptionPage, deploymentDetails.description)
            deploymentStatus = deploymentDetails.status.flatMap(s => ApiStatus.values.find(_.toString.equalsIgnoreCase(s)))
            updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiStatusPage, deploymentStatus)
            domainSubdomain = (deploymentDetails.domain, deploymentDetails.subDomain) match {
              case (Some(domain), Some(subDomain)) => Some(ProduceApiDomainSubdomain(domain, subDomain))
              case _ => None
            }
            updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiDomainPage, domainSubdomain)
            updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiHodPage, deploymentDetails.hods.map(_.toSet))
            updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiSelectEgressPage, Option.when(deploymentDetails.hasEgress)(deploymentDetails.egress).flatten)
            egressMappings = deploymentDetails.egressMappings.map(_.map(em =>
              ProduceApiEgressPrefixMapping(em.egressPrefix, em.prefix).toString
            ))
            egressPrefixes = deploymentDetails.prefixesToRemove
            egressPrefixesMappings = (egressMappings, egressPrefixes) match {
              case (Some(mappings), Some(prefixes)) => Some(ProduceApiEgressPrefixes(prefixes, mappings))
              case _ => None
            }
            updatedAnswers <- updateAnswer(updatedAnswers, UpdateApiAddPrefixesPage, egressPrefixesMappings.exists(!_.isEmpty))
            updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiEgressPrefixesPage, egressPrefixesMappings)
            response <- sessionRepository.set(updatedAnswers)
            result = Redirect(navigator.nextPage(UpdateApiStartPage, NormalMode, updatedAnswers))
          } yield result
        ))
      } else Future.successful(errorResultBuilder.notHubMaintainable())
    }
  }

  private def updateTeamAnswer(userAnswers: UserAnswers)(implicit request: ApiRequest[?]): Future[UserAnswers] =
    (for {
      teamId <- OptionT.fromOption[Future](request.apiDetails.teamId)
      team <- OptionT(apiHubService.findTeamById(teamId))
      updatedAnswers <- OptionT.liftF(updateAnswer(userAnswers, UpdateApiTeamPage, team))
    } yield updatedAnswers)
      .getOrElse(userAnswers)

  private def updateAnswer[A](userAnswers: UserAnswers, page: QuestionPage[A], answer: A)
                             (implicit r: Request[?], w: Writes[A]) =
    Future.fromTry(userAnswers.set(page, answer))

  private def updateOptionalAnswer[A](userAnswers: UserAnswers, page: QuestionPage[A], answer: Option[A])
                                     (implicit r: Request[?], w: Writes[A]) =
    answer.fold(Future.successful(userAnswers))(updateAnswer(userAnswers, page, _))

}
