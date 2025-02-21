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

import models.api.ApiGeneration.V2
import models.{Enumerable, WithName}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

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
    Enumerable(values.map(value => value.toString -> value)*)

}

sealed trait ApiType

case object SIMPLE extends WithName("SIMPLE") with ApiType
case object ADVANCED extends WithName("ADVANCED") with ApiType

object ApiType extends Enumerable.Implicits {

  val values: Seq[ApiType] = Seq(SIMPLE, ADVANCED)

  implicit val enumerable: Enumerable[ApiType] =
    Enumerable(values.map(value => value.toString -> value)*)
}

sealed trait ApiGeneration

object ApiGeneration extends Enumerable.Implicits {

  val values: Seq[ApiGeneration] = Seq(V1, V2)

  implicit val enumerable: Enumerable[ApiGeneration] =
    Enumerable(values.map(value => value.toString -> value)*)

  case object V1 extends WithName("V1") with ApiGeneration

  case object V2 extends WithName("V2") with ApiGeneration
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
  reviewedDate: Instant,
  platform: String,
  maintainer: Maintainer,
  apiType: Option[ApiType] = None,
  apiNumber: Option[String] = None,
  apiGeneration: Option[ApiGeneration] = None,
) {

  def isSelfServe: Boolean = platform == "HIP"
  
  def isHubMaintainable: Boolean = apiGeneration.contains(V2)

  def toApiDetailSummary: ApiDetailSummary = ApiDetailSummary(this)
  
}

object ApiDetail {
  private val instantDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val customInstantFormat: Format[Instant] = Format(
    Reads(js => JsSuccess(instantDateFormatter.parse(js.as[String], Instant.from))),
    Writes(d => JsString(instantDateFormatter.format(d.atOffset(ZoneOffset.UTC))))
  )
  implicit val formatApiDetail: OFormat[ApiDetail] = {
    Json.format[ApiDetail]
  }

  private val apiDetailSummaryReads: Reads[ApiDetail] =
    ( (__ \ "id").read[String]
      ~ (__ \ "publisherReference").read[String]
      ~ (__ \ "title").read[String]
      ~ (__ \ "description").read[String]
      ~ (__ \ "version").read[String]
      ~ (__ \ "endpoints").read[Seq[Endpoint]]
      ~ (__ \ "shortDescription").readNullable[String]
      ~ Reads.pure("")
      ~ (__ \ "apiStatus").read[ApiStatus]
      ~ (__ \ "teamId").readNullable[String]
      ~ (__ \ "domain").readNullable[String]
      ~ (__ \ "subDomain").readNullable[String]
      ~ (__ \ "hods").readWithDefault[Seq[String]](List.empty)
      ~ (__ \ "reviewedDate").read[Instant]
      ~ (__ \ "platform").read[String]
      ~ (__ \ "maintainer").read[Maintainer]
      ~ (__ \ "apiType").readNullable[ApiType]
      ~ (__ \ "apiNumber").readNullable[String]
      ~ (__ \ "apiGeneration").readNullable[ApiGeneration]
    )(ApiDetail.apply)
  val formatApiDetailSummary: OFormat[ApiDetail] = OFormat[ApiDetail](
    apiDetailSummaryReads,
    Json.writes[ApiDetail]
  )
}

case class Maintainer(name: String, slackChannel: String, contactInfo: List[ContactInformation] = List.empty)

object Maintainer {
  implicit val formatMaintainer: OFormat[Maintainer] = Json.format[Maintainer]
}

case class ContactInformation(name: Option[String], emailAddress: Option[String])

object ContactInformation {
  implicit val formatContactInformation: OFormat[ContactInformation] = Json.format[ContactInformation]
}
