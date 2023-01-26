package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ApplicationsConnectorSpec.{buildConnector, toJsonString}
import models.application.{Application, Creator, NewApplication}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

class ApplicationsConnectorSpec
  extends AsyncFreeSpec
  with Matchers
  with WireMockSupport {

  "ApplicationsConnector.registerApplication" - {
    "must place the correct request and return the stored application" in {
      val newApplication = NewApplication("test-name", Creator("test-creator-email"))
      val expected = Application("test-id", newApplication)

      stubFor(
        post(urlEqualTo("/api-hub-applications/applications"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(toJsonString(newApplication))
          )
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).registerApplication(newApplication)(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "ApplicationsConnector.getApplications" - {
    "must place the correct request and return the array of applications" in {
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"))
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
"ApplicationsConnector.getApplication" - {
    "must place the correct request and return the application" in {
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"))
      val expected = application1

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications/id-1"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).getApplication("id-1")(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(expected)
      }
    }
  }

  "ApplicationsConnector.getApplication" - {
    "must return the none when application is not found" in {

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications/id-1"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).getApplication("id-1")(HeaderCarrier()) map {
        actual =>
          actual mustBe None
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

  def toJsonString(newApplication: NewApplication): String = {
    Json.toJson(newApplication).toString()
  }

  def toJsonString(application: Application): String = {
    Json.toJson(application).toString()
  }

  def toJsonString(applications: Seq[Application]): String = {
    Json.toJson(applications).toString()
  }

}
