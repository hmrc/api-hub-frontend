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

package config

import org.mockito.MockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.Configuration

class EnvironmentNamesSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  "EnvironmentNames" - {
    "must be loaded from config properly" in {
      val primaryEnvName = "primaryEnvName"
      val secondaryEnvName = "secondaryEnvName"

      val mockConfiguration = Configuration.from(Map(
        "environment-names.primary" -> primaryEnvName,
        "environment-names.secondary" -> secondaryEnvName
      ))

      val envNames = mockConfiguration.get[EnvironmentNames]("environment-names")

      envNames shouldBe EnvironmentNames(primaryEnvName, secondaryEnvName)
    }
  }
}
