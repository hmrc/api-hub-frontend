package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import config.FrontendAppConfig
import connectors.ApplicationsConnectorSpec.{ApplicationGetterBehaviours, buildConnector, toJsonString}
import models.application._
import org.scalatest.OptionValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder
import scala.concurrent.ExecutionContext
class ApplicationsConnectorSpec
  extends AsyncFreeSpec
  with Matchers
  with WireMockSupport
  with OptionValues
  with ApplicationGetterBehaviours {

  "ApplicationsConnector.registerApplication" - {
    "must place the correct request and return the stored application" in {
      val newApplication = NewApplication("test-name", Creator("test-creator-email"), Seq(TeamMember("test-creator-email")))
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

  "ApplicationsConnector.getUserApplications" - {
    "must" - {
      behave like successfulUserApplicationsGetter(true)
    }

    "must" - {
      behave like successfulUserApplicationsGetter(false)
    }
  }

  "ApplicationsConnector.getApplications" - {
    "must place the correct request and return the array of applications" in {
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
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
    "must" - {
      behave like successfulApplicationGetter(true)
    }

    "must" - {
      behave like successfulApplicationGetter(false)
    }

    "must return none when application is not found" in {
      stubFor(
        get(urlEqualTo("/api-hub-applications/applications/id-1"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).getApplication("id-1", enrich = true)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "ApplicationsConnector.deleteApplication" - {
    "must place the correct request" in {
      val id = "test-id"

      val userEmail = Some("me@test.com")
      stubFor(
        delete(urlEqualTo(s"/api-hub-applications/applications/$id"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .withRequestBody(equalToJson(Json.toJson(UserEmail(userEmail)).toString()))
          .willReturn(
            aResponse().withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).deleteApplication(id, userEmail)(HeaderCarrier()) map {
        actual =>
          actual mustBe Some(())
      }
    }

    "must return None when the application is not found" in {
      val id = "test-id"

      stubFor(
        delete(urlEqualTo(s"/api-hub-applications/applications/$id"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).deleteApplication(id, None)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "ApplicationsConnector.requestAdditionalScope" - {
    "must place the correct request and return new scope" in {
      val appId = "id-1"
      val newScope = NewScope(appId, Seq(Primary))

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$appId/environments/scopes"))
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
      val newScope = NewScope(appId, Seq(Primary))

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$appId/environments/scopes"))
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
      val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
      val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
      val expected = Seq(application1, application2)

      stubFor(
        get(urlEqualTo("/api-hub-applications/applications/pending-primary-scopes"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withBody(toJsonString(expected))
          )
      )

      buildConnector(this).pendingPrimaryScopes()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }
  }

  "ApplicationsConnector.approvePrimaryScope" - {
    "must place the correct request and return true" in {
      val appId = "app_id"
      val scope = "a_scope"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/applications/$appId/environments/primary/scopes/$scope"))
          .withRequestBody(equalToJson("{\"status\":\"APPROVED\"}"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      buildConnector(this).approvePrimaryScope(appId, scope)(HeaderCarrier()) map {
        actual =>
          actual mustBe true
      }
    }

    "must return false when applications service not found" in {
      val appId = "app_id"
      val scope = "a_scope"

      stubFor(
        put(urlEqualTo(s"/api-hub-applications/applications/$appId/environments/primary/scopes/$scope"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).approvePrimaryScope(appId, scope)(HeaderCarrier()) map {
        actual =>
          actual mustBe false
      }
    }

  }

  "ApplicationsConnector.createPrimarySecret" - {
    "must place the correct request and return the secret" in {
      val applicationId = "test-application-id"
      val expected = Secret("test-secret")

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$applicationId/environments/primary/credentials/secret"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Authorization", equalTo("An authentication token"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(expected).toString())
          )
      )

      buildConnector(this).createPrimarySecret(applicationId)(HeaderCarrier()) map {
        actual =>
          actual.value mustBe expected
      }
    }

    "must return None when the application does not exist" in {
      val applicationId = "test-application-id"

      stubFor(
        post(urlEqualTo(s"/api-hub-applications/applications/$applicationId/environments/primary/credentials/secret"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector(this).createPrimarySecret(applicationId)(HeaderCarrier()) map {
        actual =>
          actual mustBe None
      }
    }
  }

  "ApplicationsConnector.testConnectivity" - {
    "must place the correct request and return the response" in {
      val expected = "something"

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/test-connectivity"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(expected)
          )
      )

      buildConnector(this).testConnectivity()(HeaderCarrier()) map {
        actual =>
          actual mustBe expected
      }
    }

    "must handle non-200 responses" in {

      stubFor(
        get(urlEqualTo(s"/api-hub-applications/test-connectivity"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector(this).testConnectivity()(HeaderCarrier()) map {
        actual =>
          actual mustBe "Response status was 500"
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
    val crypto: ApplicationCrypto = application.injector.instanceOf[ApplicationCrypto]
    new ApplicationsConnector(httpClientV2, crypto, servicesConfig, application.injector.instanceOf[FrontendAppConfig])
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

  trait ApplicationGetterBehaviours {
    this: AsyncFreeSpec with Matchers with WireMockSupport =>

    def successfulApplicationGetter(enrich: Boolean): Unit = {
      s"must place the correct request and return the application when enrich = $enrich" in {
        val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
        val expected = application1

        stubFor(
          get(urlEqualTo(s"/api-hub-applications/applications/id-1?enrich=$enrich"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(toJsonString(expected))
            )
        )

        buildConnector(this).getApplication("id-1", enrich)(HeaderCarrier()) map {
          actual =>
            actual mustBe Some(expected)
        }
      }

    }

    def successfulUserApplicationsGetter(enrich: Boolean): Unit = {
      s"must place the correct request and return the applications for a given user when enrich = $enrich" in {
        val testEmail = "test-user-email-2"
        val application1 = Application("id-1", "test-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1")))
          .copy(teamMembers = Seq(TeamMember("test-creator-email-1"), TeamMember(testEmail)))
        val application2 = Application("id-2", "test-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2")))
          .copy(teamMembers = Seq(TeamMember(testEmail), TeamMember("test-user-email-3")))
        val expected = Seq(application1, application2)
        val crypto = new ApplicationCrypto(ConfigFactory.parseResources("application.conf"))

        val userEmailEncrypted = crypto.QueryParameterCrypto.encrypt(PlainText(testEmail)).value
        val userEmailEncoded = URLEncoder.encode(userEmailEncrypted, "UTF-8")
        stubFor(
          get(urlEqualTo(f"/api-hub-applications/applications/?teamMember=$userEmailEncoded&enrich=$enrich"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("An authentication token"))
            .willReturn(
              aResponse()
                .withBody(toJsonString(expected))
            )
        )

        buildConnector(this).getUserApplications("test-user-email-2", enrich)(HeaderCarrier()) map {
          actual =>
            actual mustBe expected
        }
      }
    }

  }

}
