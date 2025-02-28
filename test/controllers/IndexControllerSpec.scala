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

import base.SpecBase
import config.{BaseHipEnvironment, FrontendAppConfig, ShareableHipConfig}
import controllers.IndexControllerSpec.{buildFixture, buildViewModel}
import controllers.actions.FakeUser
import fakes.FakeHipEnvironments
import generators.{ApiDetailGenerators, TeamGenerator}
import models.api.ApiDetail
import models.application.{Application, Creator, TeamMember}
import models.team.Team
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{framework, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.DashboardViewModel
import views.html.IndexView

import java.time.Instant
import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with MockitoSugar with TeamGenerator with ApiDetailGenerators with HtmlValidation {

  "Index Controller" - {

    "must return OK and the correct view when teams and applications exist for the user" in {
      val now = Instant.now()
      val testEmail = "test-email"
      val creatorEmail = "creator-email-2"
      val applications = Seq(
        Application("id-1", "app-name-1", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail))),
        Application("id-2", "app-name-2", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail)))
      )
      val teams = Seq(sampleTeam(), sampleTeam())
      val api1 = sampleApiDetail().copy(created = now.minusSeconds(20))
      val api2 = sampleApiDetail().copy(created = now.minusSeconds(40))
      val api3 = sampleApiDetail().copy(created = now)
      val apis = Seq(
        api1,
        api2,
        api3,
      )
      val sortedApis = Seq(
        api3,
        api1,
        api2
      )

      val fixture = buildFixture()

      running(fixture.application) {

        when(fixture.mockApiHubService.getApplications(eqTo(Some(testEmail)), eqTo(false))(any()))
          .thenReturn(Future.successful(applications))
        when(fixture.mockApiHubService.findTeams(eqTo(Some(testEmail)))(any()))
          .thenReturn(Future.successful(teams))
        when(fixture.mockApiHubService.getUserApis(eqTo(FakeUser))(any))
          .thenReturn(Future.successful(apis))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[IndexView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(buildViewModel(fixture.application, applications, teams, sortedApis))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view when no teams or applications exist for the user" in {
      val testEmail = "test-email"
      val applications = Seq.empty
      val teams = Seq.empty
      val apis = Seq.empty

      val fixture = buildFixture()

      running(fixture.application) {

        when(fixture.mockApiHubService.getApplications(eqTo(Some(testEmail)), eqTo(false))(any()))
          .thenReturn(Future.successful(applications))
        when(fixture.mockApiHubService.findTeams(eqTo(Some(testEmail)))(any()))
          .thenReturn(Future.successful(teams))
        when(fixture.mockApiHubService.getUserApis(eqTo(FakeUser))(any))
          .thenReturn(Future.successful(apis))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[IndexView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(buildViewModel(fixture.application, applications, teams, apis))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

  }

}

object IndexControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    mockApiHubService: ApiHubService
  )

  def buildFixture(): Fixture = {
    val mockApiHubService = mock[ApiHubService]
    
    val application =
      applicationBuilder(userAnswers = None)
        .overrides(
          bind[ApiHubService].toInstance(mockApiHubService)
        )
        .build()
    Fixture(application, mockApiHubService)
  }

  def buildViewModel(application: PlayApplication, applications: Seq[Application], teams: Seq[Team], apis: Seq[ApiDetail]): DashboardViewModel = {
    new DashboardViewModel(
      frontendAppConfig = application.injector.instanceOf[FrontendAppConfig],
      applications = applications,
      teams = teams,
      apis = apis,
      user = FakeUser
    )
  }

}
