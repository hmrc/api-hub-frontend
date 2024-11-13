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

import cats.data.EitherT
import cats.syntax.all.catsSyntaxApplicativeError
import com.google.inject.{Inject, Singleton}
import controllers.actions.{ApiAuthActionProvider, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import models.api.ApiStatus
import models.myapis.ApiDomainSubdomain
import models.myapis.produce.{ProduceApiEgressPrefixMapping, ProduceApiEgressPrefixes}
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.{Page, QuestionPage}
import pages.myapis.update.*
import play.api.i18n.I18nSupport
import play.api.libs.json.Writes
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.UpdateApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
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
      val userAnswers = UserAnswers(
        id = request.identifierRequest.user.userId,
        lastUpdated = clock.instant()
      )

      (for {
        (apiDetail, deploymentDetails) <- getDeploymentDetails(id)
        updatedAnswers <- updateAnswer(userAnswers, UpdateApiApiPage, apiDetail)
        updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiShortDescriptionPage, deploymentDetails.description)
        deploymentStatus = deploymentDetails.status.flatMap(s => ApiStatus.values.find(_.toString.equalsIgnoreCase(s)))
        updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiStatusPage, deploymentStatus)
        domainSubdomain = (deploymentDetails.domain, deploymentDetails.subDomain) match {
          case (Some(domain), Some(subDomain)) => Some(ApiDomainSubdomain(domain, subDomain))
          case _ => None
        }
        updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiDomainPage, domainSubdomain)
        updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiHodPage, deploymentDetails.hods.map(_.toSet))
        egressMappings = deploymentDetails.egressMappings.map(_.map(em =>
          ProduceApiEgressPrefixMapping(em.egressPrefix, em.prefix).toString
        ))
        egressPrefixes = deploymentDetails.prefixesToRemove
        egressPrefixesMappings = (egressMappings, egressPrefixes) match {
          case (Some(mappings), Some(prefixes)) => Some(ProduceApiEgressPrefixes(prefixes, mappings))
          case _ => None
        }
        updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiEgressPrefixesPage, egressPrefixesMappings)
        updatedAnswers <- updateOptionalAnswer(updatedAnswers, UpdateApiReviewEgressPage, egressPrefixesMappings.map(!_.isEmpty))
        response <- sessionRepository.set(updatedAnswers)
          .attemptT
          .leftMap(_ => errorResultBuilder.internalServerError(s"Could not save user answers"))
        result = Redirect(navigator.nextPage(UpdateApiStartPage, NormalMode, updatedAnswers))
      } yield result).merge
    }
  }

  private def getDeploymentDetails(apiId: String)(implicit r: Request[?]) =
    for {
      apiDetail <- EitherT.fromOptionF(apiHubService.getApiDetail(apiId),
        errorResultBuilder.apiNotFound(apiId)
      )
      deploymentDetails <- EitherT.fromOptionF(apiHubService.getDeploymentDetails(apiDetail.publisherReference),
        errorResultBuilder.apiNotFoundInApim(apiDetail)
      )
    } yield (apiDetail, deploymentDetails)

  private def updateAnswer[A](userAnswers: UserAnswers, page: QuestionPage[A], answer: A)
                             (implicit r: Request[?], w: Writes[A]) =
    Future.fromTry(userAnswers.set(page, answer))
          .attemptT
          .leftMap(e => errorResultBuilder.internalServerError(e))

  private def updateOptionalAnswer[A](userAnswers: UserAnswers, page: QuestionPage[A], answer: Option[A])
                                     (implicit r: Request[?], w: Writes[A]) =
    answer.fold(EitherT.rightT[Future, Result](userAnswers))(updateAnswer(userAnswers, page, _))

}
