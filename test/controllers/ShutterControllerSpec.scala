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

package controllers

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.{Application, Configuration}
import play.api.test.Helpers._

class ShutterControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  import ShutterControllerSpec._

  "ShutterController" - {
    "must display the shutter message when configured" in {
      val fixture = buildFixture()

      running(fixture.application) {
        forAll(calls) {(call: Call) =>
          val request = FakeRequest(GET, call.url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe shutterMessage
        }
      }
    }

    "must allow access to health routes" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(GET, "/ping/ping")
        val result = route(fixture.application, request).value

        status(result) mustBe OK
        contentAsString(result) must not be shutterMessage
      }
    }
  }

  private case class Fixture(
    application: Application
  )

  private def buildFixture(): Fixture = {
    Fixture(
      applicationBuilder(
        testConfiguration = Configuration.from(
          Map(
            "play.http.router" -> "shutter.Routes",
            "hubStatus.shutterMessage" -> shutterMessage
          )
        )
      ).build()
    )
  }

  private def calls = Table(
    "Call",
    controllers.routes.IndexController.onPageLoad,
    controllers.routes.ApiDetailsController.onPageLoad("test-id"),
    controllers.application.routes.ApplicationDetailsController.onPageLoad("test-id")
  )

}

object ShutterControllerSpec {

  private val shutterMessage = "test-shutter-message"

}
