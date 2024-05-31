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

package controllers

import base.OptionallyAuthenticatedSpecBase
import controllers.actions.FakeUser
import fakes.FakeDomains
import generators.ApiDetailGenerators
import models.api.{ApiDeploymentStatuses, ApiDetail}
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation
import views.html.{ApiDetailsView, ErrorTemplate}

import java.time.LocalDateTime
import scala.concurrent.Future

class ApiDetailsControllerSpec
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
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses = ApiDeploymentStatuses(Some("1"), None)

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))
          when(fixture.apiHubService.getApiDeploymentStatuses(ArgumentMatchers.eq(apiDetail.publisherReference))(any()))
            .thenReturn(Future.successful(Some(apiDeploymentStatuses)))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          val domain = FakeDomains.getDomainDescription(apiDetail)
          val subDomain = FakeDomains.getSubDomainDescription(apiDetail)

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, apiDeploymentStatuses, None, None, domain, subDomain)(request, messages(fixture.application)).toString()
          contentAsString(result) should not include "Owning team"
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Some("1"), None)

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))
          when(fixture.apiHubService.getApiDeploymentStatuses(ArgumentMatchers.eq(apiDetail.publisherReference))(any()))
            .thenReturn(Future.successful(Some(apiDeploymentStatuses)))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          val domain = FakeDomains.getDomainDescription(apiDetail)
          val subDomain = FakeDomains.getSubDomainDescription(apiDetail)

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, apiDeploymentStatuses, Some(FakeUser), None, domain, subDomain)(request, messages(fixture.application)).toString()
          contentAsString(result) should not include "Owning team"
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return a 404 Not Found page when the API detail does not exist" in {
      val fixture = buildFixture()
      val id = "test-id"

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(id))(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id $id."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must show error page when API deployment statuses cannot be retrieved" in {
      val fixture = buildFixture(userModel = Some(FakeUser))
      val apiDetail = sampleApiDetail()

      when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.getApiDeploymentStatuses(ArgumentMatchers.eq(apiDetail.publisherReference))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe view.apply(
            pageTitle = "Sorry, we are experiencing technical difficulties - 500",
            heading = "Sorry, we’re experiencing technical difficulties",
            message = "Please try again in a few minutes."
          )(request, messages(fixture.application))
          .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must display team name when api details contains a team and the team exists" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Some("1"), None)
        val team = Team("teamId", "teamName", LocalDateTime.now(), List.empty)
        val apiDetail = sampleApiDetail().copy(teamId = Some(team.id))

        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.getApiDeploymentStatuses(ArgumentMatchers.eq(apiDetail.publisherReference))(any()))
          .thenReturn(Future.successful(Some(apiDeploymentStatuses)))
        when(fixture.apiHubService.findTeamById(any())(any()))
          .thenReturn(Future.successful(Some(team)))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        val domain = FakeDomains.getDomainDescription(apiDetail)
        val subDomain = FakeDomains.getSubDomainDescription(apiDetail)

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, apiDeploymentStatuses, None, Some(team.name), domain, subDomain)(request, messages(fixture.application)).toString()
        contentAsString(result) must include("Owning team")
        contentAsString(result) must include(team.name)
        contentAsString(result) must validateAsHtml
      }
    }

    "must display error message when api details contains a team but the team cannot be retrieved" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Some("1"), None)
        val apiDetail = sampleApiDetail().copy(teamId = Some("teamId"))

        when(fixture.apiHubService.getApiDetail(ArgumentMatchers.eq(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.getApiDeploymentStatuses(ArgumentMatchers.eq(apiDetail.publisherReference))(any()))
          .thenReturn(Future.successful(Some(apiDeploymentStatuses)))
        when(fixture.apiHubService.findTeamById(any())(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        val domain = FakeDomains.getDomainDescription(apiDetail)
        val subDomain = FakeDomains.getSubDomainDescription(apiDetail)

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, apiDeploymentStatuses, None, Some("Team details could not be retrieved"), domain, subDomain)(request, messages(fixture.application)).toString()
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
