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

package controllers.application

import com.google.inject.{Inject, Singleton}
import config.{HipEnvironment, HipEnvironments}
import controllers.actions.{ApplicationAuthActionProvider, AuthorisedSupportAction, IdentifierAction}
import models.ApiWorld
import models.application.ApplicationLenses.*
import models.application.Secondary
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.*
import services.{ApiHubService, CurlCommandService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CurlCommandController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  isSupport: AuthorisedSupportAction,
  applicationAuth: ApplicationAuthActionProvider,
  apiHubService: ApiHubService,
  curlCommandService: CurlCommandService,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def buildCurlCommand(id: String, apiWorld: ApiWorld): Action[AnyContent] = (identify andThen isSupport andThen applicationAuth(id)).async {
    implicit request => {

      val application = request.application

      val testEnvironment = hipEnvironments.forEnvironmentName(Secondary)
      val eventualMaybeCredentials = apiHubService.fetchCredentials(application.id, testEnvironment)

      for {
        maybeCredentials <- apiHubService.fetchCredentials(application.id, testEnvironment)
        apiDetails <- Future.sequence(application.apis.map(api => apiHubService.getApiDetail(api.id))).map(_.flatten)
      } yield (maybeCredentials, apiDetails) match
        case (Some(credentials), apiDetails) =>
          val curlCommandResults = apiDetails.map(apiDetail => curlCommandService.buildCurlCommandsForApi(application.setCredentials(testEnvironment, credentials), apiDetail, apiWorld))
          val curlCommands = curlCommandResults.collect { case Right(curlCommand) => curlCommand }.flatten
          val errors = curlCommandResults.collect { case Left(error) => error }

          (curlCommands, errors) match {
            case (Nil, Nil) => Ok(Json.arr()) // The application has no APIs
            case (Nil, _) => InternalServerError(Json.toJson(errors.toSet.toSeq.mkString(","))) // No curl commands were built
            case _ => Ok(Json.toJson(curlCommands.map(_.toString))) // Some curl commands were successfully built
          }
        case (None, _) => Ok(Json.arr()) // Couldn't get credentials
      }
    }
}
