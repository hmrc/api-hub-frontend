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

package models.accessRequest

import models.{Enumerable, WithName}

sealed trait AccessRequestStatus

case object Pending extends WithName("PENDING") with AccessRequestStatus
case object Approved extends WithName("APPROVED") with AccessRequestStatus
case object Rejected extends WithName("REJECTED") with AccessRequestStatus
case object Cancelled extends WithName("CANCELLED") with AccessRequestStatus

object AccessRequestStatus extends Enumerable.Implicits {

  val values: Seq[AccessRequestStatus] = Seq(Pending, Approved, Rejected, Cancelled)

  implicit val enumerable: Enumerable[AccessRequestStatus] =
    Enumerable(values.map(value => value.toString -> value): _*)

}
