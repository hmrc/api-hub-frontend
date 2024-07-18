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

package models.api

import models.{Enumerable, WithName}
import play.api.libs.json._

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

sealed trait ApiStatus

case object Alpha extends WithName("ALPHA") with ApiStatus
case object Beta extends WithName("BETA") with ApiStatus
case object Live extends WithName("LIVE") with ApiStatus
case object Deprecated extends WithName("DEPRECATED") with ApiStatus

object ApiStatus extends Enumerable.Implicits {

  val values: Seq[ApiStatus] = Seq(Alpha, Beta, Live, Deprecated)

  implicit val enumerable: Enumerable[ApiStatus] =
    Enumerable(values.map(value => value.toString -> value): _*)

}

case class ApiDetail(
  id: String,
  publisherReference: String,
  title: String,
  description: String,
  version: String,
  endpoints: Seq[Endpoint],
  shortDescription: Option[String],
  openApiSpecification: String,
  apiStatus: ApiStatus,
  teamId: Option[String] = None,
  domain: Option[String] = None,
  subDomain: Option[String] = None,
  hods: Seq[String] = List.empty,
  reviewedDate: Instant
)

object ApiDetail {
  implicit val formatApiDetail: OFormat[ApiDetail] = {
    val instantDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    implicit val customInstantFormat: Format[Instant] = Format(
      Reads(js => JsSuccess(instantDateFormatter.parse(js.as[String], Instant.from))),
      Writes(d => JsString(instantDateFormatter.format(d.atOffset(ZoneOffset.UTC))))
    )
    Json.format[ApiDetail]
  }
}
