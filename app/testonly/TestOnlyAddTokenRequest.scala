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

package testonly

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class TestOnlyAddTokenRequest(
  token      : Option[Token],
  identifier : String,
  email      : String,
  permissions: Set[Permission]
)

case class TokenData(
  principal  : String,
  permissions: Set[Permission],
  email      : String
)

object TestOnlyAddTokenRequest {
  val format: OFormat[TestOnlyAddTokenRequest] = {
    implicit val pf: OFormat[Permission] = Permission.format
    ( (__ \ "token"      ).formatNullable[String].inmap[Option[Token]](_.map(Token.apply), _.map(_.value))
      ~ (__ \ "principal"  ).format[String]
      ~ (__ \ "email").format[String]
      ~ (__ \ "permissions").format[Set[Permission]]
      )(TestOnlyAddTokenRequest.apply, o => Tuple.fromProductTyped(o))
  }
}

object TokenData {
  val format: OFormat[TokenData] = {
    implicit val pf: OFormat[Permission] = Permission.format
    ( (__ \ "principal"  ).format[String]
      ~ (__ \ "permissions").format[Set[Permission]]
      ~ (__ \ "email").format[String]
      )(TokenData.apply, o => Tuple.fromProductTyped(o))
  }
}
