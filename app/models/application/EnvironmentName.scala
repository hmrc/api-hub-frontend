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

sealed trait EnvironmentName

case object Dev extends WithName("dev") with EnvironmentName
case object Test extends WithName("test") with EnvironmentName
case object PreProd extends WithName("preProd") with EnvironmentName
case object Prod extends WithName("prod") with EnvironmentName

object EnvironmentName extends Enumerable.Implicits {

  val values: Seq[EnvironmentName] = Seq(Dev, Test, PreProd, Prod)

  implicit val enumerable: Enumerable[EnvironmentName] =
    Enumerable(values.map(value => value.toString -> value): _*)

}
