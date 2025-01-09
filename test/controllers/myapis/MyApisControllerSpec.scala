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

package controllers.myapis

import base.SpecBase
import controllers.actions.FakeUser
import generators.ApiDetailGenerators
import models.api.{ApiDetail, Live, Maintainer}
import models.application.TeamMember
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.HtmlValidation
import views.html.myapis.MyApisView

import java.time.Instant
import scala.concurrent.Future

class MyApisControllerSpec
  extends SpecBase
    with MockitoSugar
    with ScalaCheckDrivenPropertyChecks
    with ApiDetailGenerators
    with HtmlValidation
    with OptionValues {

  "onPageLoad" - {
    "must return OK and the correct view when the APIS exist for an authenticated user" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[MyApisView]

        forAll { (apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getUserApis(eqTo(TeamMember(FakeUser.email)))(any))
            .thenReturn(Future.successful(Seq(apiDetail)))

          val request = FakeRequest(GET, controllers.myapis.routes.MyApisController.onPageLoad().url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(Seq(apiDetail), FakeUser)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when an unauthenticated user has no apis or teams" in {
      val fixture = buildFixture()

      running(fixture.application) {
        when(fixture.apiHubService.getUserApis(eqTo(TeamMember(FakeUser.email)))(any))
          .thenReturn(Future.successful(Seq.empty))

        val request = FakeRequest(GET, controllers.myapis.routes.MyApisController.onPageLoad().url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include("You have no APIs, or do not belong to any teams.")
        contentAsString(result) must validateAsHtml
      }
    }
  }

  "must return the apis in case-insensitive alphabetical order" in {
    val fixture = buildFixture()

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[MyApisView]

      val zebras = ApiDetail("id1", "ref1", "zebras", "zebras api", "1.0.0", Seq.empty, None, "oas", Live,
        reviewedDate = Instant.now(), platform = "HIP", maintainer = Maintainer("name", "#slack", List.empty))
      val molluscs = ApiDetail("id2", "ref2", "MOLLUSCS", "molluscs api", "1.0.0", Seq.empty, None, "oas", Live,
        reviewedDate = Instant.now(), platform = "HIP", maintainer = Maintainer("name", "#slack", List.empty))
      val aardvarks = ApiDetail("id3", "ref3", "aardvarks", "aardvarks api", "1.0.0", Seq.empty, None, "oas", Live,
        reviewedDate = Instant.now(), platform = "HIP", maintainer = Maintainer("name", "#slack", List.empty))
      val pigeons = ApiDetail("id4", "ref4", "PIGEONS", "pigeons api", "1.0.0", Seq.empty, None, "oas", Live,
        reviewedDate = Instant.now(), platform = "HIP", maintainer = Maintainer("name", "#slack", List.empty))

      when(fixture.apiHubService.getUserApis(eqTo(TeamMember(FakeUser.email)))(any))
        .thenReturn(Future.successful(Seq(molluscs, zebras, aardvarks, pigeons)))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApisController.onPageLoad().url)
      val result = route(fixture.application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(Seq(aardvarks, molluscs, pigeons, zebras), FakeUser)(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }


  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(apiHubService, application)
  }
}
