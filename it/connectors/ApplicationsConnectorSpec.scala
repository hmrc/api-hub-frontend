package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ApplicationsConnectorSpec.{buildConnector, toJsonString}
import models.application.Application
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

class ApplicationsConnectorSpec
  extends AsyncFreeSpec
  with Matchers
  with WireMockSupport {

  "ApplicationsConnector.createApplication" - {
    "must place the correct request and return the stored application" in {
      val application = Application(None, "test-name")
      val expected = application.copy(id = Some("test-id"))

      stubFor(
        post(urlEqualTo("/api-hub-applications/applications"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(toJsonString(application))
          )
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).createApplication(application)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "ApplicationsConnector.getApplications" - {
    "must place the correct request and return the array of applications" in {
      val application1 = Application(Some("id-1"), "test-name-1")
      val application2 = Application(Some("id-2"), "test-name-2")
      val expected = Seq(application1, application2)

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).getApplications()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }
}

object ApplicationsConnectorSpec extends HttpClientV2Support {

  def buildConnector(wireMockSupport: WireMockSupport)(implicit ec: ExecutionContext): ApplicationsConnector = {
    val servicesConfig = new ServicesConfig(
      Configuration.from(Map(
        "microservice.services.api-hub-applications.host" -> wireMockSupport.wireMockHost,
        "microservice.services.api-hub-applications.port" -> wireMockSupport.wireMockPort
      ))
    )

    new ApplicationsConnector(httpClientV2, servicesConfig)
  }

  def toJsonString(application: Application): String = {
    Json.toJson(application).toString()
  }

  def toJsonString(applications: Seq[Application]): String = {
    Json.toJson(applications).toString()
  }

}
