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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.FrontendAppConfig
import generators.ApiDetailGenerators
import models.api.IntegrationResponse
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class IntegrationCatalogueConnectorSpec
  extends AsyncFreeSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with ApiDetailGenerators {

  "getApiDetail" - {
    "must place the correct request and return the API detail" in {
      val expected = sampleApiDetail()

      stubFor(
        get(urlEqualTo(s"/integration-catalogue/integrations/${expected.id}"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector().getApiDetail(expected.id)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }

    "must return None when the API detail is not found" in {
      val expected = sampleApiDetail()

      stubFor(
        get(urlEqualTo(s"/integration-catalogue/integrations/${expected.id}"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().getApiDetail(expected.id)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }

    "must fail with an exception when integration catalogue returns a failure response" in {
      val expected = sampleApiDetail()

      stubFor(
        get(urlEqualTo(s"/integration-catalogue/integrations/${expected.id}"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      recoverToSucceededIf[UpstreamErrorResponse] {
        buildConnector().getApiDetail(expected.id)(HeaderCarrier())
      }
    }

    "getHipApis" - {
      "must place the correct request and return some ApiDetails" in {
        val expected = sampleApis()

        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations?platformFilter=hip&integrationType=api"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().getAllHipApis()(HeaderCarrier()) map {
          actual =>
            actual mustBe expected.results
        }
      }

      "Must return empty Seq if no search results" in {
        val expected = IntegrationResponse(0,None, Seq.empty)

        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations?platformFilter=hip&integrationType=api"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().getAllHipApis()(HeaderCarrier()) map {
          actual =>
            actual mustBe Seq.empty
        }
      }

      "must fail with an exception when integration catalogue returns a failure response" in {
        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations"))
            .withQueryParam("platformFilter", equalTo("hip"))
            .withQueryParam("integrationType", equalTo("api"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        recoverToSucceededIf[UpstreamErrorResponse] {
          buildConnector().getAllHipApis()(HeaderCarrier())
        }
      }
    }

    "filterApis" - {
      "must place the correct request and return some ApiDetails" in {
        val expected = sampleApis()

        stubFor(
          get(urlEqualTo("/integration-catalogue/integrations?teamIds=team2&teamIds=team1"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().filterApis(Seq("team1", "team2"))(HeaderCarrier()) map {
          actual =>
            actual mustBe expected.results
        }
      }

      "Must return empty Seq if no search results" in {
        val expected = IntegrationResponse(0,None, Seq.empty)

        stubFor(
          get(urlEqualTo("/integration-catalogue/integrations?teamIds=team2&teamIds=team1"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().filterApis(Seq("team1", "team2"))(HeaderCarrier()) map {
          actual =>
            actual mustBe Seq.empty
        }
      }

      "must fail with an exception when integration catalogue returns a failure response" in {
        stubFor(
          get(urlEqualTo("/integration-catalogue/integrations?teamIds=team2&teamIds=team1"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        recoverToSucceededIf[UpstreamErrorResponse] {
          buildConnector().filterApis(Seq("team1", "team2"))(HeaderCarrier())
        }
      }
    }
  }

  "updateApiTeam" - {
    val teamId = "team1"
    val apiDetails = sampleApiDetail().copy(teamId = Some(teamId))

    "must place the correct request and return an ApiDetail" in {
      stubFor(
        put(urlEqualTo(s"/integration-catalogue/apis/${apiDetails.id}/teams/$teamId"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(apiDetails).toString())
          )
      )

      buildConnector().updateApiTeam(apiDetails.id, teamId)(HeaderCarrier()) map {
        actual =>
          actual mustBe apiDetails
      }
    }

    "must fail with an exception when integration catalogue returns a failure response" in {
      stubFor(
        put(urlEqualTo(s"/integration-catalogue/apis/${apiDetails.id}/teams/$teamId"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      recoverToSucceededIf[UpstreamErrorResponse] {
        buildConnector().updateApiTeam(apiDetails.id, teamId)(HeaderCarrier())
      }
    }
  }

  private def buildConnector(): IntegrationCatalogueConnector = {
    val servicesConfig = new ServicesConfig(
      Configuration.from(Map(
        "microservice.services.integration-catalogue.host" -> wireMockHost,
        "microservice.services.integration-catalogue.port" -> wireMockPort
      ))
    )

    val application = new GuiceApplicationBuilder().build()
    new IntegrationCatalogueConnector(httpClientV2, servicesConfig, application.injector.instanceOf[FrontendAppConfig])
  }

}
