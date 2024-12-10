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

package models.application

import play.api.libs.json.{Format, Json}

import java.time.LocalDateTime
import scala.math.Ordered.orderingToOrdered

case class CredentialScopes(
                             environmentId: String,
                             clientId: String,
                             created: LocalDateTime,
                             scopes: Seq[String]
)

object CredentialScopes {

  implicit val formatCredentialScopes: Format[CredentialScopes] = Json.format[CredentialScopes]

  implicit val orderingCredentialScopes: Ordering[CredentialScopes] = (x: CredentialScopes, y: CredentialScopes) => {
    (x.environmentId, x.created) compare (y.environmentId, y.created)
  }

}
