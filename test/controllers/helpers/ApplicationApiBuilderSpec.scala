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

package controllers.helpers

import base.SpecBase
import controllers.actions.FakeApplication
import models.accessrequest.{AccessRequest, Pending}
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live, Maintainer}
import models.application.{Api, Scope, SelectedEndpoint}
import models.application.ApplicationLenses.ApplicationLensOps
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.Request
import play.api.mvc.Results.NotFound
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import viewmodels.application.{Accessible, ApplicationApi, ApplicationEndpoint, Inaccessible, Requested}
import views.html.ErrorTemplate

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

class ApplicationApiBuilderSpec extends SpecBase with MockitoSugar {

  import ApplicationApiBuilderSpec._

  "ApplicationApiBuilder" - {
    "must correctly stitch together data" in {
      val fixture = buildFixture()
      val application = FakeApplication
        .addApi(Api(apiId1, apiTitle1, Seq(SelectedEndpoint("GET", "/test1/1"), SelectedEndpoint("POST", "/test1/1"), SelectedEndpoint("GET", "/test1/2"))))
        .addApi(Api(apiId2, apiTitle2, Seq(SelectedEndpoint("GET", "/test2/1"))))
        .addApi(Api(apiId3, apiTitle3, Seq(SelectedEndpoint("GET", "/test3/1"))))
        .setPrimaryScopes(scopes("all:test-scope-1", "get:test-scope-1-1", "get:test-scope-1-2"))
        .setSecondaryScopes(scopes("all:test-scope-1", "get:test-scope-1-1", "post:test-scope-1-1", "get:test-scope-1-2", "get:test-scope-3-1"))

      when(fixture.apiHubService.getApiDetail(eqTo(apiId1))(any()))
        .thenReturn(Future.successful(Some(apiDetail1)))
      when(fixture.apiHubService.getApiDetail(eqTo(apiId2))(any()))
        .thenReturn(Future.successful(Some(apiDetail2)))
      when(fixture.apiHubService.getApiDetail(eqTo(apiId3))(any()))
        .thenReturn(Future.successful(Some(apiDetail3)))
      when(fixture.apiHubService.getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(Some(Pending)))(any()))
        .thenReturn(Future.successful(Seq(accessRequest)))

      running(fixture.application) {
        implicit val request: Request[?] = FakeRequest()

        val actual = fixture.applicationApiBuilder.build(application).futureValue

        val expected = Seq(
          ApplicationApi(
            apiDetail1,
            Seq(
              ApplicationEndpoint("GET", "/test1/1", None, None, Seq("all:test-scope-1", "get:test-scope-1-1"), Accessible, Accessible),
              ApplicationEndpoint("POST", "/test1/1", None, None, Seq("all:test-scope-1", "post:test-scope-1-1"), Inaccessible, Accessible),
              ApplicationEndpoint("GET", "/test1/2", None, None, Seq("all:test-scope-1", "get:test-scope-1-2"), Accessible, Accessible)
            ),
            false
          ),
          ApplicationApi(
            apiDetail2,
            Seq(
              ApplicationEndpoint("GET", "/test2/1", None, None, Seq("get:test-scope-2-1"), Inaccessible, Inaccessible)
            ),
            false
          ),
          ApplicationApi(
            apiDetail3,
            Seq(
              ApplicationEndpoint("GET", "/test3/1", None, None, Seq("get:test-scope-3-1"), Requested, Accessible)
            ),
            true
          )
        )

        actual mustBe Right(expected)
      }
    }

    "must return an empty sequence of stitched data when the application has no APIs added" in {
      val fixture = buildFixture()
      val application = FakeApplication

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(Some(Pending)))(any()))
        .thenReturn(Future.successful(Seq.empty))

      running(fixture.application) {
        implicit val request: Request[?] = FakeRequest()

        val actual = fixture.applicationApiBuilder.build(application).futureValue

        actual mustBe Right(Seq.empty)
      }
    }

    "must return a 404 Not Found result when an API detail cannot be found" in {
      val fixture = buildFixture()
      val apiId = "test-id"
      val application = FakeApplication.addApi(Api(apiId, "test-title", Seq.empty))

      when(fixture.apiHubService.getAccessRequests(eqTo(Some(FakeApplication.id)), eqTo(Some(Pending)))(any()))
        .thenReturn(Future.successful(Seq.empty))
      when(fixture.apiHubService.getApiDetail(any())(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        implicit val request: Request[?] = FakeRequest()
        implicit val msgs: Messages = messages(fixture.application)

        val actual = fixture.applicationApiBuilder.build(application).futureValue
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        val expected = NotFound(
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id $apiId."
          )
        )

        actual mustBe Left(expected)
      }
    }
  }

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
      )
      .build()

    val applicationApiBuilder = application.injector.instanceOf[ApplicationApiBuilder]

    Fixture(application, apiHubService, applicationApiBuilder)
  }

}

object ApplicationApiBuilderSpec {

  private case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService,
    applicationApiBuilder: ApplicationApiBuilder
  )

  private val apiId1 = "test-id-1"
  private val apiId2 = "test-id-2"
  private val apiId3 = "test-id-3"

  private val apiTitle1 = "test-title-1"
  private val apiTitle2 = "test-title-2"
  private val apiTitle3 = "test-title-3"

  private val apiDetail1 =
    ApiDetail(
      id = apiId1,
      publisherReference = "test-publisher-reference-1",
      title = "test-title-1",
      description = "test-description-1",
      version = "test-version-1",
      endpoints = Seq(
        Endpoint(
          path = "/test1/1",
          methods = Seq(
            EndpointMethod("GET", None, None, Seq("all:test-scope-1", "get:test-scope-1-1")),
            EndpointMethod("POST", None, None, Seq("all:test-scope-1", "post:test-scope-1-1"))
          )
        ),
        Endpoint(
          path = "/test1/2",
          methods = Seq(
            EndpointMethod("GET", None, None, Seq("all:test-scope-1", "get:test-scope-1-2"))
          )
        )
      ),
      shortDescription = None,
      openApiSpecification = "test-oas-spec-1",
      apiStatus = Live,
      reviewedDate = Instant.now(),
      platform = "HIP",
      maintainer = Maintainer("name", "#slack", List.empty)
    )

  private val apiDetail2 =
    ApiDetail(
      id = apiId2,
      publisherReference = "test-publisher-reference-2",
      title = "test-title-2",
      description = "test-description-2",
      version = "test-version-2",
      endpoints = Seq(
        Endpoint(
          path = "/test2/1",
          methods = Seq(
            EndpointMethod("GET", None, None, Seq("get:test-scope-2-1")),
            EndpointMethod("POST", None, None, Seq("post:test-scope-2-1"))
          )
        ),
        Endpoint(
          path = "/test2/2",
          methods = Seq(
            EndpointMethod("GET", None, None, Seq("get:test-scope-2-2"))
          )
        )
      ),
      shortDescription = None,
      openApiSpecification = "test-oas-spec-2",
      apiStatus = Live,
      reviewedDate = Instant.now(),
      platform = "HIP",
      maintainer = Maintainer("name", "#slack", List.empty)
    )

  private val apiDetail3 =
    ApiDetail(
      id = apiId3,
      publisherReference = "test-publisher-reference-3",
      title = "test-title-3",
      description = "test-description-3",
      version = "test-version-3",
      endpoints = Seq(
        Endpoint(
          path = "/test3/1",
          methods = Seq(
            EndpointMethod("GET", None, None, Seq("get:test-scope-3-1"))
          )
        )
      ),
      shortDescription = None,
      openApiSpecification = "test-oas-spec-3",
      apiStatus = Live,
      reviewedDate = Instant.now(),
      platform = "HIP",
      maintainer = Maintainer("name", "#slack", List.empty)
    )

  private def accessRequest = AccessRequest(
    id = "test-access-request-id",
    applicationId = FakeApplication.id,
    apiId = apiDetail3.id,
    apiName = apiDetail3.title,
    status = Pending,
    supportingInformation = "test-supporting-information",
    requested = LocalDateTime.now,
    requestedBy = "test-requested-by"
  )

  private def scope(name: String): Scope = {
    Scope(name)
  }

  private def scopes(name: String *): Seq[Scope] = {
    name.map(scope)
  }

}
