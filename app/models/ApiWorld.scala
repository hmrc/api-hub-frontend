/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc.QueryStringBindable

sealed trait ApiWorld

case object MDTP extends ApiWorld
case object CORPORATE extends ApiWorld

object ApiWorld {
  val values: Seq[ApiWorld] = Seq(MDTP, CORPORATE)

  implicit val enumerable: Enumerable[ApiWorld] =
    Enumerable(values.map(value => value.toString -> value) *)

  implicit val queryStringBindable: QueryStringBindable[ApiWorld] = new QueryStringBindable[ApiWorld] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiWorld]] = {
      for {
        apiWorldName <- params
          .find(_._1.toLowerCase == "world")
          .flatMap(_._2.headOption)
          .map(_.toUpperCase)
        apiWorld <- enumerable.withName(apiWorldName) match {
          case Some(apiWorld) => Some(Right(apiWorld))
          case None => Some(Left(s"Unknown API World $apiWorldName"))
        }
      } yield apiWorld
    }

    override def unbind(key: String, value: ApiWorld): String = {
      s"world=${value.toString}"
    }
  }
}
