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
import config.FrontendAppConfig
import controllers.actions.FakeUser
import fakes.{FakeDomains, FakeHipEnvironments, FakeHods, FakePlatforms}
import generators.ApiDetailGenerators
import models.api.ApiDeploymentStatus.*
import models.api.{ApiDeploymentStatuses, ApiDetail, ContactInfo, ContactInformation, PlatformContact}
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
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
import viewmodels.{ApiTeamContactEmail, HubSupportContactEmail, NonSelfServeApiViewModel, SelfServeApiViewModel}
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
    "must return OK and the correct view when the API detail exists for an unauthenticated user and the API is self-serve" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses = ApiDeploymentStatuses(Seq(
          Deployed(FakeHipEnvironments.test.id, "1"),
          Deployed(FakeHipEnvironments.production.id, "1"),
        ))

        forAll {(baseApiDetail: ApiDetail) =>
          val apiDetail = baseApiDetail.copy(platform = "HIP")
          when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))
          when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
            .thenReturn(Future.successful(apiDeploymentStatuses))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          val domain = FakeDomains.getDomainDescription(apiDetail)
          val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
          val hods = apiDetail.hods.map(FakeHods.getDescription(_))
          val platform = FakePlatforms.getDescription(apiDetail.platform)
          val apiView = SelfServeApiViewModel(domain, subDomain, hods, platform, None, apiDeploymentStatuses)

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an unauthenticated user and the API is " +
      "non-self-serve and there is no email address for the API team" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val config = fixture.application.injector.instanceOf[FrontendAppConfig]
        val apiDeploymentStatuses = ApiDeploymentStatuses(Seq(
          Deployed(FakeHipEnvironments.test.id, "1"),
          Deployed(FakeHipEnvironments.production.id, "1"),
        ))

        forAll {(baseApiDetail: ApiDetail) =>
          val apiDetail = baseApiDetail.copy(platform = "OTHER", maintainer = baseApiDetail.maintainer.copy(contactInfo = List.empty))
          when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))
          when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
            .thenReturn(Future.successful(apiDeploymentStatuses))
          when(fixture.apiHubService.getPlatformContact(any)(any, any)).thenReturn(Future.successful(None))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          val domain = FakeDomains.getDomainDescription(apiDetail)
          val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
          val hods = apiDetail.hods.map(FakeHods.getDescription(_))
          val platform = FakePlatforms.getDescription(apiDetail.platform)
          val apiView = NonSelfServeApiViewModel(domain, subDomain, hods, platform, HubSupportContactEmail(config.supportEmailAddress))

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an unauthenticated user and the API is " +
      "non-self-serve and there is an email address for the API team" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses = ApiDeploymentStatuses(Seq(
          Deployed(FakeHipEnvironments.test.id, "1"),
          Deployed(FakeHipEnvironments.production.id, "1"),
        ))
        val apiTeamEmail = "team@example.com"

        forAll {(baseApiDetail: ApiDetail) =>
          val apiDetail = baseApiDetail.copy(platform = "OTHER",
            maintainer = baseApiDetail.maintainer.copy(contactInfo = List(ContactInformation(name=None, emailAddress = Some(apiTeamEmail)))))
          when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))
          when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
            .thenReturn(Future.successful(apiDeploymentStatuses))
          when(fixture.apiHubService.getPlatformContact(any)(any, any)).thenReturn(Future.successful(Some(PlatformContact("", ContactInfo("", "team@example.com"), false))))

          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          val domain = FakeDomains.getDomainDescription(apiDetail)
          val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
          val hods = apiDetail.hods.map(FakeHods.getDescription(_))
          val platform = FakePlatforms.getDescription(apiDetail.platform)
          val apiView = NonSelfServeApiViewModel(domain, subDomain, hods, platform, ApiTeamContactEmail(apiTeamEmail))

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return OK and the correct view when the API detail exists for an authenticated user" in {
      val fixture = buildFixture(userModel = Some(FakeUser))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val config = fixture.application.injector.instanceOf[FrontendAppConfig]

        forAll {(apiDetail: ApiDetail) =>
          when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
            .thenReturn(Future.successful(Some(apiDetail)))

          when(fixture.apiHubService.getPlatformContact(any)(any, any)).thenReturn(Future.successful(None))
          val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
          val result = route(fixture.application, request).value

          val domain = FakeDomains.getDomainDescription(apiDetail)
          val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
          val hods = apiDetail.hods.map(FakeHods.getDescription(_))
          val platform = FakePlatforms.getDescription(apiDetail.platform)
          val apiView = NonSelfServeApiViewModel(domain, subDomain, hods, platform, HubSupportContactEmail(config.supportEmailAddress))

          status(result) mustBe OK
          contentAsString(result) mustBe view(apiDetail, Some(FakeUser), apiView)(request, messages(fixture.application)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must return a 404 Not Found page when the API detail does not exist" in {
      val fixture = buildFixture()
      val id = "test-id"

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        when(fixture.apiHubService.getApiDetail(eqTo(id))(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(id).url)
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with ID $id."
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must display team name when api details contains a team and the team exists" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Seq(
          NotDeployed(FakeHipEnvironments.test.id),
          Deployed(FakeHipEnvironments.production.id, "1"),
        ))
        val team = Team("teamId", "teamName", LocalDateTime.now(), List.empty)
        val apiDetail = sampleApiDetail().copy(teamId = Some(team.id), platform = "HIP")

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
          .thenReturn(Future.successful(apiDeploymentStatuses))
        when(fixture.apiHubService.findTeamById(any())(any()))
          .thenReturn(Future.successful(Some(team)))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        val domain = FakeDomains.getDomainDescription(apiDetail)
        val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
        val hods = apiDetail.hods.map(FakeHods.getDescription(_))
        val platform = FakePlatforms.getDescription(apiDetail.platform)
        val apiView = SelfServeApiViewModel(domain, subDomain, hods, platform, Some(team.name), apiDeploymentStatuses)

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
        contentAsString(result) must include("Owning team")
        contentAsString(result) must include(team.name)
        contentAsString(result) must validateAsHtml
      }
    }

    "must display error message when api details contains a team but the team cannot be retrieved" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Seq(
          NotDeployed(FakeHipEnvironments.test.id),
          Deployed(FakeHipEnvironments.production.id, "1"),
        ))
        val apiDetail = sampleApiDetail().copy(teamId = Some("teamId"), platform = "HIP")

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
          .thenReturn(Future.successful(apiDeploymentStatuses))
        when(fixture.apiHubService.findTeamById(any())(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        val domain = FakeDomains.getDomainDescription(apiDetail)
        val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
        val hods = apiDetail.hods.map(FakeHods.getDescription(_))
        val platform = FakePlatforms.getDescription(apiDetail.platform)
        val apiView = SelfServeApiViewModel(domain, subDomain, hods, platform, Some("Team details could not be retrieved"), apiDeploymentStatuses)

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must display not deployed message when there isn't an api deployment" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Seq(
          NotDeployed(FakeHipEnvironments.test.id),
          Deployed(FakeHipEnvironments.production.id, "1"),
        ))
        val apiDetail = sampleApiDetail().copy(teamId = Some("teamId"), platform = "HIP")

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
          .thenReturn(Future.successful(apiDeploymentStatuses))
        when(fixture.apiHubService.findTeamById(any())(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        val domain = FakeDomains.getDomainDescription(apiDetail)
        val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
        val hods = apiDetail.hods.map(FakeHods.getDescription(_))
        val platform = FakePlatforms.getDescription(apiDetail.platform)
        val apiView = SelfServeApiViewModel(domain, subDomain, hods, platform, Some("Team details could not be retrieved"), apiDeploymentStatuses)

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
        contentAsString(result) must include("Not deployed")
      }
    }

    "must display unknown message when the api deployment status is unknown" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ApiDetailsView]
        val apiDeploymentStatuses =  ApiDeploymentStatuses(Seq(
          Unknown(FakeHipEnvironments.test.id),
          Unknown(FakeHipEnvironments.production.id),
        ))
        val apiDetail = sampleApiDetail().copy(teamId = Some("teamId"), platform = "HIP")

        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any()))
          .thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any()))
          .thenReturn(Future.successful(apiDeploymentStatuses))
        when(fixture.apiHubService.findTeamById(any())(any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ApiDetailsController.onPageLoad(apiDetail.id).url)
        val result = route(fixture.application, request).value

        val domain = FakeDomains.getDomainDescription(apiDetail)
        val subDomain = FakeDomains.getSubDomainDescription(apiDetail)
        val hods = apiDetail.hods.map(FakeHods.getDescription(_))
        val platform = FakePlatforms.getDescription(apiDetail.platform)
        val apiView = SelfServeApiViewModel(domain, subDomain, hods, platform, Some("Team details could not be retrieved"), apiDeploymentStatuses)

        status(result) mustBe OK
        contentAsString(result) mustBe view(apiDetail, None, apiView)(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
        contentAsString(result) must include("Unable to retrieve the API status at the moment. Try again later.")
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
