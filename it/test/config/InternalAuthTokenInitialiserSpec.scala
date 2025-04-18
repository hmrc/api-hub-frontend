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

import com.github.tomakehurst.wiremock.client.WireMock.*
import fakes.FakeHubStatusService
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.AUTHORIZATION
import services.HubStatusService
import util.WireMockHelper

import scala.util.Try

class InternalAuthTokenInitialiserSpec extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience with WireMockHelper {

  "when configured to run" - {

    "must initialise the internal-auth token if it is not already initialised" in {

      val authToken = "authToken1"
      val appName = "appName1"

      val expectedRequest = Json.obj(
        "token" -> authToken,
        "principal" -> appName,
        "permissions" -> Seq(
          Json.obj(
            "resourceType" -> "api-hub-applications",
            "resourceLocation" -> "*",
            "actions" -> List("READ","WRITE","DELETE")
          ),
          Json.obj(
            "resourceType" -> "integration-catalogue",
            "resourceLocation" -> "*",
            "actions" -> List("READ","WRITE","DELETE")
          )
        )
      )

      server.stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      server.stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )


      GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> server.port(),
          "appName" -> appName,
          "create-internal-auth-token-on-start" -> true,
          "internal-auth.token" -> authToken
        )
        .overrides(bind[HubStatusService].toInstance(FakeHubStatusService))
        .build()

      eventually(Timeout(Span(30, Seconds))) {
        server.verify(1,
          getRequestedFor(urlMatching("/test-only/token"))
            .withHeader(AUTHORIZATION, equalTo(authToken))
        )
        server.verify(1,
          postRequestedFor(urlMatching("/test-only/token"))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(expectedRequest))))
        )
      }
      
    }

    "must return an exception if the internal auth service responds to create with a different status" in {

      val authToken = "authToken2"
      val appName = "appName2"

      val expectedRequest = Json.obj(
        "token" -> authToken,
        "principal" -> appName,
        "permissions" -> Seq(
          Json.obj(
            "resourceType" -> "digital-disclosure-service",
            "resourceLocation" -> "*",
            "actions" -> List("WRITE")
          ),
          Json.obj(
            "resourceType" -> "digital-disclosure-service-store",
            "resourceLocation" -> "*",
            "actions" -> List("WRITE")
          )
        )
      )

      server.stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      server.stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val appTry = Try{
        GuiceApplicationBuilder()
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "create-internal-auth-token-on-start" -> true,
            "internal-auth.token" -> authToken
          )
          .overrides(bind[HubStatusService].toInstance(FakeHubStatusService))
          .build()

        eventually(Timeout(Span(30, Seconds))) {
          server.verify(1,
            getRequestedFor(urlMatching("/test-only/token"))
              .withHeader(AUTHORIZATION, equalTo(authToken))
          )
          server.verify(1,
            postRequestedFor(urlMatching("/test-only/token"))
              .withRequestBody(equalToJson(Json.stringify(Json.toJson(expectedRequest))))
          )
        }
      }

      appTry.failed
    }

    "must not initialise the internal-auth token if it is already initialised" in {

      val authToken = "authToken"
      val appName = "appName"

      server.stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(OK))
      )

      server.stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )

      val app = GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> server.port(),
          "appName" -> appName,
          "create-internal-auth-token-on-start" -> true,
          "internal-auth.token" -> authToken
        )
        .overrides(bind[HubStatusService].toInstance(FakeHubStatusService))
        .build()

      app.injector.instanceOf[InternalAuthTokenInitialiser].initialised.futureValue

      server.verify(1,
        getRequestedFor(urlMatching("/test-only/token"))
          .withHeader(AUTHORIZATION, equalTo(authToken))
      )
      server.verify(0,
        postRequestedFor(urlMatching("/test-only/token"))
      )
    }
  }

  "when not configured to run" - {

    "must not make the relevant calls to internal-auth" in {

      val authToken = "authToken"
      val appName = "appName"

      server.stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(OK))
      )

      server.stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )

      val app = GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> server.port(),
          "appName" -> appName,
          "create-internal-auth-token-on-start" -> false,
          "internal-auth.token" -> authToken
        )
        .overrides(bind[HubStatusService].toInstance(FakeHubStatusService))
        .build()

      app.injector.instanceOf[InternalAuthTokenInitialiser].initialised.futureValue

      server.verify(0,
        getRequestedFor(urlMatching("/test-only/token"))
      )
      server.verify(0,
        postRequestedFor(urlMatching("/test-only/token"))
      )
    }
  }

}