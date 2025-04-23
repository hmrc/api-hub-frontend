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

package controllers.apis

import com.google.inject.{Inject, Singleton}
import config.*
import controllers.actions.OptionalIdentifierAction
import controllers.helpers.ErrorResultBuilder
import models.api.ApiDetail
import models.requests.OptionalIdentifierRequest
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.*
import views.html.apis.ApiDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDetailsController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  view: ApiDetailsView,
  errorResultBuilder: ErrorResultBuilder,
  optionallyIdentified: OptionalIdentifierAction,
  domains: Domains,
  hods: Hods,
  platforms: Platforms,
  frontendAppConfig: FrontendAppConfig,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      for {
        maybeApiDetail <- apiHubService.getApiDetail(id)
        result <- maybeApiDetail match {
          case Some(apiDetail) if apiDetail.isSelfServe(platforms) => processSelfServeApiDetail(apiDetail)
          case Some(apiDetail) => processNonSelfServeApiDetail(apiDetail)
          case None =>
            Future.successful(errorResultBuilder.notFound(
              Messages("site.apiNotFound.heading"),
              Messages("site.apiNotFound.message", id)
            ))
        }
      } yield result
  }

  private def processSelfServeApiDetail(apiDetail: ApiDetail)(implicit request: OptionalIdentifierRequest[?]) = {
    for {
      apiDeploymentStatuses <- apiHubService.getApiDeploymentStatuses(apiDetail.publisherReference)
        .map(_.sortStatusesWithHipEnvironments(hipEnvironments))
      maybeTeamName <- getTeamNameForApi(apiDetail.teamId)
    } yield
        Ok(view(
          apiDetail,
          request.user,
          SelfServeApiViewModel(
            domains.getDomainDescription(apiDetail),
            domains.getSubDomainDescription(apiDetail),
            apiDetail.hods.map(hods.getDescription(_)),
            platforms.getDescription(apiDetail.platform),
            maybeTeamName,
            apiDeploymentStatuses,
          )
        ))
  }

  private def processNonSelfServeApiDetail(apiDetail: ApiDetail)(implicit request: OptionalIdentifierRequest[?]) = {
    getContactEmailAddress(apiDetail.platform).flatMap(
      contactEmail => Future.successful(Ok(view(
        apiDetail,
        request.user,
        NonSelfServeApiViewModel(
          domains.getDomainDescription(apiDetail),
          domains.getSubDomainDescription(apiDetail),
          apiDetail.hods.map(hods.getDescription(_)),
          platforms.getDescription(apiDetail.platform),
          contactEmail
        )
      ))))
  }

  private def getTeamNameForApi(maybeTeamId: Option[String])(implicit request: OptionalIdentifierRequest[?]) = {
    maybeTeamId match {
      case Some(teamId) => apiHubService.findTeamById(teamId).map {
        case Some(team) => Some(team.name)
        case None => Some(Messages("apiDetails.details.team.error"))
      }
      case None => Future.successful(None)
    }
  }

  private def getContactEmailAddress(forPlatform: String)(implicit request: OptionalIdentifierRequest[?]): Future[ApiContactEmail] = {
    val eventualMaybeContact = apiHubService.getPlatformContact(forPlatform)
    eventualMaybeContact map  {
      case Some(platformContact) =>
        ApiTeamContactEmail(platformContact.contactInfo.emailAddress)
      case _ =>
        HubSupportContactEmail(frontendAppConfig.supportEmailAddress)
    }
  }

}
