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

package controllers.admin

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthorisedSupportAction, IdentifierAction}
import models.application.Application
import models.application.ApplicationLenses._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.admin.TeamMigrationView

import scala.concurrent.ExecutionContext

@Singleton
class TeamMigrationController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  apiHubService: ApiHubService,
  view: TeamMigrationView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import TeamMigrationController._

  def onPageLoad(): Action[AnyContent] = (identify andThen isSupport).async {
    implicit request =>
      apiHubService.getApplications(None, true).map(
        applications =>
          Ok(view(
            applications.filterNot(_.isTeamMigrated).sortBy(_.name.toLowerCase),
            MigrationSummary(applications),
            TeamApplications(applications),
            request.user
          ))
      )
  }

}

object TeamMigrationController {

  case class MigrationSummary(migrated: Boolean, deleted: Boolean, count: Int)

  object MigrationSummary {

    def apply(applications: Seq[Application]): Seq[MigrationSummary] = {
      applications
        .groupMapReduce(
          application =>
            (application.isTeamMigrated, application.isDeleted)
        )(_ => 1)(_ + _)
        .toSeq
        .map { case ((isMigrated, isDeleted), count) =>
            MigrationSummary(isMigrated, isDeleted, count)
        }
    }

  }

  case class TeamApplications(teamMembers: Seq[String], applications: Seq[Application])

  object TeamApplications {

    def apply(applications: Seq[Application]): Seq[TeamApplications] = {
      applications
        .filterNot(_.isTeamMigrated)
        .groupMapReduce(
          application =>
            application.teamMembers.map(_.email).toSet
        )(a => Seq(a))((b1, b2) => b1 ++ b2)
        .map {
          case (emails, applications) =>
            TeamApplications(
              emails.toSeq.sortBy(_.toLowerCase),
              applications.sortBy(_.name.toLowerCase)
            )
        }
        .toSeq
        .sortBy(_.teamMembers.map(_.toLowerCase).mkString(" "))
    }

  }

}
