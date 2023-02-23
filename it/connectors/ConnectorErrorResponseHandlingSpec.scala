package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.application.{Application, Creator}
import models.errors.{ApplicationNameNotUnique, ErrorResponse, RequestError}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import scala.concurrent.Future

class ConnectorErrorResponseHandlingSpec extends AsyncFreeSpec with Matchers with WireMockSupport with HttpClientV2Support {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "ConnectorErrorResponseHandling.responseBodyToModel" - {
    "must return a valid model" in {
      val application = Application("test-id", "test-name", Creator("test-email"))

      stubFor(
        post(urlEqualTo("/test-connector"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(application).toString())
          )
      )

      new TestConnector(httpClientV2).postMethod[Application]().map {
        result =>
          result mustBe Right(Some(application))
      }
    }

    "must throw ConnectorException when no response body is returned" in {
      stubFor(
        post(urlEqualTo("/test-connector"))
          .willReturn(
            aResponse()
          )
      )

      recoverToSucceededIf[ConnectorException] {
        new TestConnector(httpClientV2).postMethod[Application]()
      }
    }

    "must throw ConnectorException if the body does not match the model" in {
      stubFor(
        post(urlEqualTo("/test-connector"))
          .willReturn(
            aResponse()
              .withBody("{}")
          )
      )

      recoverToSucceededIf[ConnectorException] {
        new TestConnector(httpClientV2).postMethod[Application]()
      }
    }
  }

  "ConnectorErrorResponseHandling.badRequest" - {
    "must return the request error when recoverable" in {
      stubFor(
        post(urlEqualTo("/test-connector"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(Json.toJson(ErrorResponse(ApplicationNameNotUnique, "")).toString())
          )
      )

      new TestConnector(httpClientV2).postMethod[Application]().map {
        result =>
          result mustBe Left(ApplicationNameNotUnique)
      }
    }
  }

  class TestConnector(httpClient: HttpClientV2) extends ConnectorErrorResponseHandling with Logging {

    private implicit val writesUnit: Writes[Unit] = (_: Unit) => JsObject(Seq.empty)

    def postMethod[T : Reads]()(implicit hc: HeaderCarrier): Future[Either[RequestError, Option[T]]] = {
      val url = new URL("http", wireMockHost, wireMockPort, "/test-connector")

      httpClient
        .post(url)
        .execute[HttpResponse]
        .flatMap {
          response =>
            response.status match {
              case success if HttpErrorFunctions.is2xx(success) => responseBodyToModel[T](response).map(application => Right(Some(application)))
              case NOT_FOUND => Future.successful(Right(None))
              case BAD_REQUEST => badRequest(response).map(Left(_))
              case _ => Future.failed(connectorException(response))
            }
        }
    }

  }

}
