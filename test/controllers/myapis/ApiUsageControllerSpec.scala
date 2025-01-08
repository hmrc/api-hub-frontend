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
import controllers.actions.{FakeSupporter, FakeUser}
import controllers.routes
import fakes.FakeHipEnvironments
import generators.ApiDetailGenerators
import models.api.ApiDeploymentStatuses
import models.api.ApiDeploymentStatus.*
import models.application.ApplicationLenses.*
import models.application.{Api, Application, Creator, Deleted, TeamMember}
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.myapis.ApiUsageView

import java.time.LocalDateTime
import scala.concurrent.Future

class ApiUsageControllerSpec
  extends SpecBase
    with MockitoSugar
    with ApiDetailGenerators
    with HtmlValidation {

  "must return OK and the correct view for a support user" in {
    val fixture = buildFixture(FakeSupporter)
    val teamId = "teamId"
    val apiDetail = sampleApiDetail().copy(teamId = Some(teamId))
    val apps = Seq(
      Application("id-1", "test-app-name-1", Creator("test-creator-email-1"), Seq(TeamMember("test-creator-email-1"))).addApi(Api("apiId", "apiTitle")),
      Application("id-2", "test-app-name-2", Creator("test-creator-email-2"), Seq(TeamMember("test-creator-email-2"))).addApi(Api("apiId", "apiTitle"))
        .delete(Deleted(LocalDateTime.now(), "deletingUser"))
    )
    val owningTeam = Team(teamId, "teamName", LocalDateTime.now(), List.empty)

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[ApiUsageView]

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.findTeamById(eqTo(teamId))(any)).thenReturn(Future.successful(Some(owningTeam)))
      when(fixture.apiHubService.getApplicationsUsingApi(eqTo(apiDetail.id), eqTo(true))(any))
        .thenReturn(Future.successful(apps))
      val statuses = ApiDeploymentStatuses(Seq(
        Deployed(FakeHipEnvironments.production.id, "1"),
        Deployed(FakeHipEnvironments.test.id, "1")
      ))
      when(fixture.apiHubService.getApiDeploymentStatuses(any)(any)).thenReturn(Future.successful(statuses))
      
      val request = FakeRequest(GET, controllers.myapis.routes.ApiUsageController.onPageLoad(apiDetail.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(apiDetail, Some(owningTeam), apps, FakeSupporter, statuses)(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  "must redirect to Unauthorised page for a non-support user who is a member of the api team" in {
    val fixture = buildFixture()
    val apiTeam = Team("teamId", "teamName", LocalDateTime.now(), List.empty)
    val apiDetail = sampleApiDetail().copy(teamId = Some(apiTeam.id))

    running(fixture.application) {
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any))
        .thenReturn(Future.successful(List(apiTeam)))

      val request = FakeRequest(GET, controllers.myapis.routes.ApiUsageController.onPageLoad(apiDetail.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }
  }

  "must return Not Found if the API does not exist" in {
    val fixture = buildFixture(FakeSupporter)
    val missingApiId = "not-there"

    running(fixture.application) {
      when(fixture.apiHubService.getApiDetail(eqTo(missingApiId))(any))
        .thenReturn(Future.successful(None))
      val view = fixture.application.injector.instanceOf[ErrorTemplate]

      val request = FakeRequest(GET, controllers.myapis.routes.ApiUsageController.onPageLoad(missingApiId).url)
      val result = route(fixture.application, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe view(
        "Page not found - 404",
        "API not found",
        s"Cannot find an API with ID $missingApiId.",
        Some(FakeSupporter)
      )(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  private case class Fixture(apiHubService: ApiHubService, application: PlayApplication)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = userModel)
      .overrides(bind[ApiHubService].toInstance(apiHubService))
      .build()

    Fixture(apiHubService, application)
  }

}
