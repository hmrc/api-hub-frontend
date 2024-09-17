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

import com.google.inject.Inject
import controllers.actions.{ApplicationAuthActionProvider, IdentifierAction}
import controllers.helpers.ApplicationApiBuilder
import models.application.ApplicationLenses.*
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.ApplicationDetailsView
import models.api.ApiDetail
import scala.concurrent.ExecutionContext
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.oas.models.{Paths,PathItem,Operation}
import scala.jdk.CollectionConverters.*
import models.ApiWorld

class CurlCommandController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  applicationAuth: ApplicationAuthActionProvider,
  view: ApplicationDetailsView,
  applicationApiBuilder: ApplicationApiBuilder
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def buildCurlCommand(id: String, apiWorld: ApiWorld): Action[AnyContent] = (identify andThen applicationAuth(id, enrich = true)).async {
    implicit request =>
      applicationApiBuilder.build(request.application).map {
        case Right(applicationApis) =>
          Ok(Json.toJson(applicationApis.flatMap(api => parseApiOasSpec(api.apiDetail))))
        case Left(_) => Ok(Json.toJson(""))
      }
  }

  private def parseApiOasSpec(apiDetail: ApiDetail): Seq[String] = {
    val options: ParseOptions = new ParseOptions()

    options.setResolve(false)
    val result = new OpenAPIV3Parser().readContents(apiDetail.openApiSpecification, null, options)
    val openApiDoc = result.getOpenAPI
    val paths = openApiDoc.getPaths

    getCurlCommandsForPaths(paths)
  }

  private def getCurlCommandsForPaths(paths: Paths): Seq[String] = {
    val x = paths.keySet().asScala.toSeq
    x.flatMap(path => getCurlCommandsForPath(path, paths.get(path)))
  }

  private def getCurlCommandsForPath(path: String, pathItem: PathItem): Seq[String] = {
    Map(
      "GET" -> pathItem.getGet,
      "POST" -> pathItem.getPost,
      "PUT" -> pathItem.getPut,
      "DELETE" -> pathItem.getDelete,
      "OPTIONS" -> pathItem.getOptions,
      "HEAD" -> pathItem.getHead,
      "PATCH" -> pathItem.getPatch,
      "TRACE" -> pathItem.getTrace
    )
      .filter { case (_, operation) => operation != null }
      .flatMap { case (method, operation) => getCurlCommandForOperation(method, path, pathItem, operation) }
      .toSeq

  }

  private def getCurlCommandForOperation(method: String, path: String, pathItem: PathItem, operation: Operation): Seq[String] = {
    Seq(s"curl -X '${method}' '${path}'")
  }

}

