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
import play.api.libs.json.{Format, Json}

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
  teamId: Option[String] = None
)

object ApiDetail {

  implicit val formatApiDetail: Format[ApiDetail] = Json.format[ApiDetail]

}
