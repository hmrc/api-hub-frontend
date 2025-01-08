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

package models.api

import fakes.FakeHipEnvironments
import models.api.ApiDeploymentStatus.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class ApiDeploymentStatusSpec extends AnyFreeSpec with Matchers {

  "ApiDeploymentStatus" - {
    "should parse the Deployed status" in {
      val json =
        """
          |{
          |  "environmentId": "production",
          |  "version": "1",
          |  "_type": "Deployed"
          |}
          |""".stripMargin
      val expected: ApiDeploymentStatus = Deployed(FakeHipEnvironments.production.id, "1")

      val deployed = Json.parse(json).as[ApiDeploymentStatus]

      deployed mustBe expected
    }

    "should parse the NotDeployed status" in {
      val json =
        """
          |{
          |  "environmentId": "production",
          |  "_type": "NotDeployed"
          |}
          |""".stripMargin
      val expected = NotDeployed(FakeHipEnvironments.production.id)

      val notDeployed = Json.parse(json).as[ApiDeploymentStatus]

      notDeployed mustBe expected
    }

    "should parse the Unknown status" in {
      val json =
        """
          |{
          |  "environmentId": "production",
          |  "_type": "Unknown"
          |}
          |""".stripMargin
      val expected = Unknown(FakeHipEnvironments.production.id)

      val unknown = Json.parse(json).as[ApiDeploymentStatus]

      unknown mustBe expected
    }
  }

}
