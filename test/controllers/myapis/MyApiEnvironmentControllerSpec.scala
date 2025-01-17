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
import config.FrontendAppConfig
import controllers.actions.{FakeSupporter, FakeUser}
import controllers.routes
import fakes.FakeHipEnvironments
import generators.ApiDetailGenerators
import models.api.ApiDeploymentStatus.{Deployed, NotDeployed}
import models.api.ApiDeploymentStatuses
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.myapis.MyApiEnvironmentView

import java.time.LocalDateTime
import scala.concurrent.Future

class MyApiEnvironmentControllerSpec
  extends SpecBase
    with MockitoSugar
    with ApiDetailGenerators
    with HtmlValidation {

  "must return OK and the correct view for a user on the api team" in {
    val fixture = buildFixture()
    val teamId = "teamId"
    val teamName = "teamName"
    val apiTeam = Team(teamId, teamName, LocalDateTime.now(), List.empty)
    val apiDetail = sampleApiDetail().copy(teamId = Some(apiTeam.id))
    val deploymentStatuses = ApiDeploymentStatuses(Seq(
      Deployed(FakeHipEnvironments.production.id, "1.0"),
      Deployed(FakeHipEnvironments.test.id, "1.0")
    ))

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[MyApiEnvironmentView]

      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any))
        .thenReturn(Future.successful(deploymentStatuses))
      when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any))
        .thenReturn(Future.successful(List(apiTeam)))
      when(fixture.apiHubService.findTeamById(eqTo(teamId))(any))
        .thenReturn(Future.successful(Some(apiTeam)))
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, FakeHipEnvironments.test.id).url)
      val result = route(fixture.application, request).value

      val r = status(result) 
//      status(result) mustBe OK
      contentAsString(result) mustBe view(apiDetail, FakeHipEnvironments.test, FakeUser, deploymentStatuses)(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  "must return OK and the correct view for a support user not on the api team" in {
    val fixture = buildFixture(FakeSupporter)
    val apiDetail = sampleApiDetail()
    val deploymentStatuses = ApiDeploymentStatuses(Seq(
      Deployed(FakeHipEnvironments.production.id, "1.0"),
      Deployed(FakeHipEnvironments.test.id, "1.0")
    ))

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[MyApiEnvironmentView]

      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any))
        .thenReturn(Future.successful(deploymentStatuses))
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
      val request = FakeRequest(GET, controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, FakeHipEnvironments.test.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(apiDetail, FakeHipEnvironments.test, FakeSupporter, deploymentStatuses)(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  "must redirect to Unauthorised page for a non-support user not on the api team" in {
    val fixture = buildFixture()
    val apiDetail = sampleApiDetail()

    running(fixture.application) {
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, FakeHipEnvironments.test.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }
  }

  "must return Not Found if the API does not exist" in {
    val fixture = buildFixture()
    val missingApiId = "not-there"

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[ErrorTemplate]

      when(fixture.apiHubService.getApiDetail(eqTo(missingApiId))(any)).thenReturn(Future.successful(None))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(missingApiId, FakeHipEnvironments.test.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe view(
        "Page not found - 404",
        "API not found",
        s"Cannot find an API with ID $missingApiId.",
        Some(FakeUser)
      )(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }  
  
  "must return Not Found if the environment does not exist" in {
    val fixture = buildFixture(FakeSupporter)
    val missingEnvironment = "nope"
    val apiDetail = sampleApiDetail()
    
    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[ErrorTemplate]

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
      
      val request = FakeRequest(GET, controllers.myapis.routes.MyApiEnvironmentController.onPageLoad(apiDetail.id, missingEnvironment).url)
      val result = route(fixture.application, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe view(
        "Page not found - 404",
        "Environment not found",
        s"Cannot find environment $missingEnvironment.",
        Some(FakeSupporter)
      )(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = userModel)
      .overrides(bind[ApiHubService].toInstance(apiHubService))
      .build()

    Fixture(apiHubService, application)
  }

}
