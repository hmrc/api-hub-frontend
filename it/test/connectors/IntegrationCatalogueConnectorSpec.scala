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

import com.github.tomakehurst.wiremock.client.WireMock.*
import config.FrontendAppConfig
import fakes.FakeHubStatusService
import generators.ApiDetailGenerators
import models.api.{ApiDetail, ContactInfo, IntegrationPlatformReport, IntegrationResponse, PlatformContact}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.HubStatusService
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

    "getApis" - {
      "must place the correct request and return some ApiDetails if no platform filter supplied" in {
        val expected = sampleApiDetailSummaries()

        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().getApis(None)(HeaderCarrier()) map {
          actual =>
            actual mustBe expected
        }
      }

      "must place the correct request and return some ApiDetails if platform filter is supplied" in {
        val expected = sampleApiDetailSummaries()

        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries?platformFilter=hip"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().getApis(Some("hip"))(HeaderCarrier()) map {
          actual =>
            actual mustBe expected
        }
      }

      "Must return empty Seq if no search results" in {
        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.arr().toString)
            )
        )

        buildConnector().getApis(None)(HeaderCarrier()) map {
          actual =>
            actual mustBe Seq.empty
        }
      }

      "must fail with an exception when integration catalogue returns a failure response" in {
        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries"))
            .withQueryParam("platformFilter", equalTo("hip"))
            .withQueryParam("integrationType", equalTo("api"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        recoverToSucceededIf[UpstreamErrorResponse] {
          buildConnector().getApis(None)(HeaderCarrier())
        }
      }
    }

    "filterApis" - {
      "must place the correct request and return some ApiDetails" in {
        val expected = sampleApis()

        stubFor(
          get(urlEqualTo("/integration-catalogue/integrations?integrationType=api&teamIds=team2&teamIds=team1"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().filterApis(Seq("team1", "team2"))(HeaderCarrier()) map {
          actual =>
            actual mustBe expected.results.map(_.copy(openApiSpecification = ""))
        }
      }

      "Must return empty Seq if no search results" in {
        val expected = IntegrationResponse(0,None, Seq.empty)

        stubFor(
          get(urlEqualTo("/integration-catalogue/integrations?integrationType=api&teamIds=team2&teamIds=team1"))
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

    "deepSearch" - {
      "must place the correct request and return some ApiDetails" in {
        val expected = sampleApiDetailSummaries()
        val searchTerm = "nps"

        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries?searchTerm=$searchTerm"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().deepSearchApis(searchTerm)(HeaderCarrier()) map {
          actual =>
            actual mustBe expected
        }
      }

      "Must return empty Seq if no search results" in {
        val noResultsTerm = "nope"

        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries?searchTerm=$noResultsTerm"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.arr().toString())
            )
        )

        buildConnector().deepSearchApis(noResultsTerm)(HeaderCarrier()) map {
          actual =>
            actual mustBe Seq.empty
        }
      }

      "must fail with an exception when integration catalogue returns a failure response" in {
        val searchTerm = "nps"
        stubFor(
          get(urlEqualTo(s"/integration-catalogue/integrations/summaries?searchTerm=$searchTerm"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        recoverToSucceededIf[UpstreamErrorResponse] {
          buildConnector().deepSearchApis(searchTerm)(HeaderCarrier())
        }
      }
    }

    "getPlatformContacts" - {
      "must place the correct request and return some PlatformContacts" in {
        val expected = Seq(
          PlatformContact("A_PLATFORM", ContactInfo("a name", "an email"), false),
          PlatformContact("ANOTHER_PLATFORM", ContactInfo("another name", "another email"), false)
        )

        stubFor(
          get(urlEqualTo("/integration-catalogue/platform/contacts"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(Json.toJson(expected).toString())
            )
        )

        buildConnector().getPlatformContacts()(HeaderCarrier()) map {
          actual =>
            actual mustBe expected
        }
      }

      "Must return empty Seq if no search results" in {
        stubFor(
          get(urlEqualTo("/integration-catalogue/platform/contacts"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody("[]")
            )
        )

        buildConnector().getPlatformContacts()(HeaderCarrier()) map {
          actual =>
            actual mustBe Seq.empty
        }
      }

      "must fail with an exception when integration catalogue returns a failure response" in {
        stubFor(
          get(urlEqualTo("/integration-catalogue/platform/contacts"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        recoverToSucceededIf[UpstreamErrorResponse] {
          buildConnector().getPlatformContacts()(HeaderCarrier())
        }
      }
    }
  }

  "getApiDetailForPublishReference" - {
    "must place the correct request and return the API detail" in {
      val expected = sampleApiDetail()

      stubFor(
        get(urlEqualTo(s"/integration-catalogue/integrations/publisher-reference/${expected.publisherReference}"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector().getApiDetailForPublishReference(expected.publisherReference)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }

    "must return None when the API detail is not found" in {
      val expected = sampleApiDetail()

      stubFor(
        get(urlEqualTo(s"/integration-catalogue/integrations/publisher-reference/${expected.publisherReference}"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().getApiDetailForPublishReference(expected.publisherReference)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "getReport" - {
    "must place the correct request and return the response" in {
      val expected = Seq(
        IntegrationPlatformReport(
          platformType = "HIP",
          integrationType = ApiDetail.IntegrationType.api,
          count = 12
        ),
        IntegrationPlatformReport(
          platformType = "NOT-HIP",
          integrationType = ApiDetail.IntegrationType.api,
          count = 23
        )
      )

      stubFor(
        get(urlEqualTo(s"/integration-catalogue/report"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector().getReport()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
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

    val application = new GuiceApplicationBuilder()
      .overrides(bind[HubStatusService].toInstance(FakeHubStatusService))
      .build()
    new IntegrationCatalogueConnector(httpClientV2, servicesConfig, application.injector.instanceOf[FrontendAppConfig])
  }

}
