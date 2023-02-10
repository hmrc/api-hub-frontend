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

case class Retrievals(principal: String, email: Option[String], canApprove: Boolean)

object Retrievals {

  private val reads: Reads[Retrievals] = (
    (JsPath \ "retrievals")(0).read[String] and
      (JsPath \ "retrievals")(1).readNullable[String] and
      (JsPath \ "retrievals")(2).read[Boolean]
  )(Retrievals.apply _)

//  private val writes: Writes[Retrievals] = (
//    (JsPath \ "retrievals")(0).write[String] and
//      (JsPath \ "retrievals")(1).writeNullable[String]and
//      (JsPath \ "retrievals")(2).write[Boolean]
//  )(unlift(Retrievals.unapply))

  private val writes: Writes[Retrievals] = new Writes[Retrievals] {
    override def writes(retrievals: Retrievals): JsValue = {
      Json.obj(
        "retrievals" -> JsArray(
          Seq(
            JsString(retrievals.principal),
            retrievals.email.map(JsString).getOrElse(JsNull),
            JsBoolean(retrievals.canApprove)
          )
        )
      )
    }
  }

  implicit val formatRetrievals: Format[Retrievals] = Format(reads, writes)

}
