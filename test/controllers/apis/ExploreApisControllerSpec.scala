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

package controllers.apis

import base.OptionallyAuthenticatedSpecBase
import controllers.actions.FakeUser
import fakes.{FakeDomains, FakeHods, FakePlatforms}
import generators.ApiDetailGenerators
import models.api.{ApiDetail, ApiDetailSummary, Live, Maintainer}
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.HtmlValidation
import views.html.apis.ExploreApisView

import java.time.Instant
import scala.concurrent.Future

class ExploreApisControllerSpec
  extends OptionallyAuthenticatedSpecBase
    with MockitoSugar
    with ScalaCheckDrivenPropertyChecks
    with ApiDetailGenerators
    with HtmlValidation
    with OptionValues {

  "GET" - {
    "must return OK and the correct view when the API detail exists for an unauthenticated user" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ExploreApisView]

        forAll { (apiDetail: ApiDetailSummary) =>
          when(fixture.apiHubService.getApis(any())(any()))
            .thenReturn(Future.successful(Seq(apiDetail)))

          val request = FakeRequest(GET, controllers.apis.routes.ExploreApisController.onPageLoad().url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(None, Seq(apiDetail), FakeDomains, FakeHods, FakePlatforms)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ExploreApisView]

        forAll { (apiDetail: ApiDetailSummary) =>
          when(fixture.apiHubService.getApis(any())(any()))
            .thenReturn(Future.successful(Seq(apiDetail)))

          val request = FakeRequest(GET, controllers.apis.routes.ExploreApisController.onPageLoad().url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(Some(FakeUser), Seq(apiDetail), FakeDomains, FakeHods, FakePlatforms)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return the apis in case-insensitive alphabetical order" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ExploreApisView]

        val platform = "HIP"
        val maintainer = Maintainer("name", "#slack", List.empty)
        val zebras = ApiDetail("id1", "ref1", "zebras", "zebras api", "1.0.0", Seq.empty, None, "oas", Live, created = Instant.now().minusSeconds(10), reviewedDate = Instant.now(), platform = platform, maintainer = maintainer).toApiDetailSummary(FakePlatforms)
        val molluscs = ApiDetail("id2", "ref2", "MOLLUSCS", "molluscs api", "1.0.0", Seq.empty, None, "oas", Live, created = Instant.now().minusSeconds(20), reviewedDate = Instant.now(), platform = platform, maintainer = maintainer).toApiDetailSummary(FakePlatforms)
        val aardvarks = ApiDetail("id3", "ref3", "aardvarks", "aardvarks api", "1.0.0", Seq.empty, None, "oas", Live, created = Instant.now().minusSeconds(30), reviewedDate = Instant.now(), platform = platform, maintainer = maintainer).toApiDetailSummary(FakePlatforms)
        val pigeons = ApiDetail("id4", "ref4", "PIGEONS", "pigeons api", "1.0.0", Seq.empty, None, "oas", Live, created = Instant.now().minusSeconds(40), reviewedDate = Instant.now(), platform = platform, maintainer = maintainer).toApiDetailSummary(FakePlatforms)

        when(fixture.apiHubService.getApis(any())(any()))
          .thenReturn(Future.successful(Seq(molluscs, zebras, aardvarks, pigeons)))

        val request = FakeRequest(GET, controllers.apis.routes.ExploreApisController.onPageLoad().url)
        val result = route(fixture.application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(None, Seq(aardvarks, molluscs, pigeons, zebras), FakeDomains, FakeHods, FakePlatforms)(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }
 }

  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: Option[UserModel] = None): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(apiHubService, application)
  }
}
