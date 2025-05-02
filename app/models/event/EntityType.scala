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

package models.event

import models.{Enumerable, WithName}


sealed trait EntityType

case object Application extends WithName("APPLICATION") with EntityType
case object Team  extends WithName("TEAM") with EntityType
case object Api extends WithName("API") with EntityType

object EntityType extends Enumerable.Implicits {

  val values: Seq[EntityType] = Seq(Application, Team, Api)

  implicit val enumerable: Enumerable[EntityType] =
    Enumerable(values.map(value => value.toString -> value)*)

}
