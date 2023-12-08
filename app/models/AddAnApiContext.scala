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

package models

import play.api.mvc.JavascriptLiteral

sealed trait AddAnApiContext

case object AddAnApi extends WithName("add-an-api") with AddAnApiContext
case object AddEndpoints extends WithName("add-endpoints") with AddAnApiContext

object AddAnApiContext extends Enumerable.Implicits {

  val values: Seq[AddAnApiContext] = Seq(AddAnApi, AddEndpoints)

  implicit val enumerable: Enumerable[AddAnApiContext] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit val jsLiteral: JavascriptLiteral[AddAnApiContext]= new JavascriptLiteral[AddAnApiContext]() {
    override def to(value: AddAnApiContext): String = value match {
      case AddAnApi => AddAnApi.toString
      case AddEndpoints => AddEndpoints.toString
    }
  }

}
