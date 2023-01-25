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

package models.application

import models.{Enumerable, WithName}

sealed trait ScopeStatus

case object Pending extends WithName("PENDING") with ScopeStatus
case object Approved extends WithName("APPROVED") with ScopeStatus
case object Denied extends WithName("DENIED") with ScopeStatus

object ScopeStatus extends Enumerable.Implicits {

  val values: Seq[ScopeStatus] = Seq(Pending, Approved, Denied)

  implicit val enumerable: Enumerable[ScopeStatus] =
    Enumerable(values.map(value => value.toString -> value): _*)

}
