package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.FrontendAppConfig
import generators.ApiDetailGenerators
import org.scalatest.OptionValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class IntegrationCatalogueConnectorSpec
  extends AsyncFreeSpec
  with Matchers
  with WireMockSupport
  with OptionValues
  with HttpClientV2Support
  with ApiDetailGenerators {

  "getApiDetail" - {
    "must place the correct request and return the API detail" in {
      val expected = arbitraryApiDetail.arbitrary.sample.value

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
