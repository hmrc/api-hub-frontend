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
import play.api.mvc.PathBindable

sealed trait EnvironmentName

case object Primary extends WithName("primary") with EnvironmentName
case object Secondary extends WithName("secondary") with EnvironmentName

object EnvironmentName extends Enumerable.Implicits {

  val values: Seq[EnvironmentName] = Seq(Primary, Secondary)

  implicit val enumerable: Enumerable[EnvironmentName] =
    Enumerable(values.map(value => value.toString -> value)*)

  implicit val pathBindableEnvironmentName: PathBindable[EnvironmentName] = new PathBindable[EnvironmentName] {
    override def bind(key: String, value: String): Either[String, EnvironmentName] = {
      enumerable
        .withName(value)
        .toRight(s"Unknown environment name: $value")
    }

    override def unbind(key: String, value: EnvironmentName): String = {
      value.toString
    }
  }
}
