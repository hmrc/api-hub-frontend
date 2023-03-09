package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.FrontendAppConfig
import connectors.ApplicationsConnectorSpec.{buildConnector, toJsonString}
import models.application._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import play.api.inject.guice.GuiceApplicationBuilder
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
          .withHeader("Authorization", equalTo("An authentication token"))
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
          .withHeader("Authorization", equalTo("An authentication token"))
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
          .withHeader("Authorization", equalTo("An authentication token"))
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

  "ApplicationsConnector.requestAdditionalScope" - {
    "must place the correct request and return new scope" in {
      val appId = "id-1"
      val newScope = NewScope(appId, Seq(Dev))

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/${appId}/environments/scopes"))
          .withRequestBody(equalToJson(Json.toJson(Seq(newScope)).toString()))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      buildConnector(this).requestAdditionalScope(appId, newScope)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(newScope)
      }
    }

    "must return empty when application ID not found" in {
      val appId = "unknown_id"
      val newScope = NewScope(appId, Seq(Dev))

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/${appId}/environments/scopes"))
          .withRequestBody(equalToJson(Json.toJson(Seq(newScope)).toString()))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      buildConnector(this).requestAdditionalScope(appId, newScope)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "ApplicationsConnector.pendingScopes" - {
    "must place the correct request and return the array of applications" in {
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"))
      val expected = Seq(application1, application2)

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications/pending-scopes"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).pendingScopes()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "ApplicationsConnector.approveProductionScope" - {
    "must place the correct request and return true" in {
      val appId = "app_id"
      val scope = "a_scope"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/applications/${appId}/environments/prod/scopes/${scope}"))
          .withRequestBody(equalToJson("{\"status\":\"APPROVED\"}"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).approveProductionScope(appId, scope)(HeaderCarrier()) map {
        actual =>
          actual mustBe true
      }
    }

    "must return false when applications service not found" in {
      val appId = "app_id"
      val scope = "a_scope"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/applications/${appId}/environments/prod/scopes/${scope}"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).approveProductionScope(appId, scope)(HeaderCarrier()) map {
        actual =>
          actual mustBe false
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

    val application = new GuiceApplicationBuilder().build()
    new ApplicationsConnector(httpClientV2, servicesConfig, application.injector.instanceOf[FrontendAppConfig])
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
