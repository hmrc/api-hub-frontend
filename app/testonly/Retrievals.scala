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

package testonly

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Retrievals(principal: String, email: String, canApprove: Boolean, canSupport: Boolean, isPrivileged: Boolean)

object Retrievals {

  private val reads: Reads[Retrievals] = (
    (JsPath \ "retrievals")(0).read[String] and
      (JsPath \ "retrievals")(1).read[String] and
      (JsPath \ "retrievals")(2).read[Boolean] and
      (JsPath \ "retrievals")(3).read[Boolean] and
      (JsPath \ "retrievals")(4).read[Boolean]
  )(Retrievals.apply)

  private val writes: Writes[Retrievals] = (retrievals: Retrievals) => {
    Json.obj(
      "retrievals" -> JsArray(
        Seq(
          JsString(retrievals.principal),
          JsString(retrievals.email),
          JsBoolean(retrievals.canApprove),
          JsBoolean(retrievals.canSupport),
          JsBoolean(retrievals.isPrivileged)
        )
      )
    )
  }

  implicit val formatRetrievals: Format[Retrievals] = Format(reads, writes)

}
