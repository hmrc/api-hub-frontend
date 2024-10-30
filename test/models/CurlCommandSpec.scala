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

import base.SpecBase
import generators.Generators
import io.swagger.v3.oas.models.servers.Server
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CurlCommandSpec extends SpecBase
  with ScalaCheckPropertyChecks
  with Generators {

  "CurlCommand" - {
    "must generate a string with the expected url" in {
      forAll(genIntersperseString(Gen.alphaLowerStr.suchThat(!_.isBlank), "/") -> "path") {
        (path: String) =>
          forAll(genIntersperseString(Gen.alphaLowerStr.suchThat(!_.isBlank), "/").map(_.prepended('/')) -> "commandPath") {
            (commandPath: String) =>
              val serverUrl = s"http://example.com/$path"
              val server = Server().url(serverUrl).description("MDTP - QA")
              val commonHeaders = Map("Content-Type" -> "application/json", "Authorization" -> "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")
              val command = CurlCommand("GET", Some(server), commandPath, Map.empty, Map.empty, commonHeaders, None)
              command.toString must include(s"$serverUrl$commandPath")
          }
      }
    }
  }
}
