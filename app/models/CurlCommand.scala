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

package models

import io.swagger.v3.oas.models.servers.Server
import models.CurlCommand.missingValue
import sttp.model.Uri

case class CurlCommand(
                        method: String,
                        server: Option[Server],
                        path: String,
                        queryParams: Map[String, String],
                        pathParams: Map[String, String],
                        headers: Map[String, String],
                        requestBody: Option[String]
                      ) {
  override def toString: String = {
    val url = Uri.unsafeParse(server.map(_.getUrl).getOrElse("http://__EXAMPLE.COM__"))
      .withWholePath(getPathWithParameters)
      .addParams(queryParams)
      .toString
    val headers = getHeadersString
    s"curl -X '${method}' $headers '$url' ${requestBody.map(json => s"--data '$json'").getOrElse("")}"
  }

  private val pathParamRegex = "\\{([^\\}]*)\\}".r

  private def getHeadersString: String = {
    headers.map { case (key, value) => s"-H '$key: $value'" }.mkString(" ")
  }

  private def getPathWithParameters: String = {
    var newPath = path
    pathParams.foreach { case (paramName, paramValue) =>
      newPath = newPath.replace(s"{$paramName}", paramValue)
    }
    pathParamRegex.replaceAllIn(newPath, m => missingValue(m.group(1)))
  }
}

object CurlCommand {
  def missingValue(valueName: String, valueType: String): String = missingValue(s"${valueName}_${valueType}")
  def missingValue(value: String): String = s"__${value.toUpperCase}__"
}