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
import generators.ApiDetailGenerators
import models.api.ApiDeploymentStatuses
import models.team.Team
import models.user.UserModel
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.myapis.MyApiDetailsView

import java.time.LocalDateTime
import scala.concurrent.Future

class MyApiDetailsControllerSpec
  extends SpecBase
    with MockitoSugar
    with ArgumentMatchersSugar
    with ApiDetailGenerators
    with HtmlValidation {

  "must return OK and the correct view for a user on the api team" in {
    val fixture = buildFixture()
    val apiTeam = Team("teamId", "teamName", LocalDateTime.now(), List.empty)
    val apiDetail = sampleApiDetail().copy(teamId = Some(apiTeam.id))
    val deploymentStatuses = ApiDeploymentStatuses(Some("1.0"), None)

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[MyApiDetailsView]

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.findTeams(eqTo(FakeUser.email))(any))
        .thenReturn(Future.successful(List(apiTeam)))
      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any))
        .thenReturn(Future.successful(Some(deploymentStatuses)))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiDetail.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(apiDetail, deploymentStatuses, Some(FakeUser))(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  "must return OK and the correct view for a support user not on the api team" in {
    val fixture = buildFixture(FakeSupporter)
    val apiDetail = sampleApiDetail()
    val deploymentStatuses = ApiDeploymentStatuses(Some("1.0"), None)

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[MyApiDetailsView]

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any))
        .thenReturn(Future.successful(Some(deploymentStatuses)))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiDetail.id).url)
      val result = route(fixture.application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(apiDetail, deploymentStatuses, Some(FakeSupporter))(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  "must redirect to Unauthorised page for a non-support user not on the api team" in {
    val fixture = buildFixture()
    val apiDetail = sampleApiDetail().copy(teamId = Some("apiTeam"))

    running(fixture.application) {
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.findTeams(eqTo(FakeUser.email))(any))
        .thenReturn(Future.successful(List(Team("userTeam", "teamName", LocalDateTime.now(), List.empty))))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiDetail.id).url)
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

      when(fixture.apiHubService.getApiDetail(eqTo(missingApiId))(any))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiDetailsController.onPageLoad(missingApiId).url)
      val result = route(fixture.application, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe view(
        "Page not found - 404",
        "API not found",
        s"Cannot find an API with Id $missingApiId.")(request, messages(fixture.application)).toString()
      contentAsString(result) must validateAsHtml
    }
  }

  "must display error page when api deployments cannot be retrieved" in {
    val fixture = buildFixture()
    val apiTeam = Team("teamId", "teamName", LocalDateTime.now(), List.empty)
    val apiDetail = sampleApiDetail().copy(teamId = Some(apiTeam.id))

    running(fixture.application) {
      val view = fixture.application.injector.instanceOf[ErrorTemplate]

      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any))
        .thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.findTeams(eqTo(FakeUser.email))(any))
        .thenReturn(Future.successful(List(apiTeam)))
      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, controllers.myapis.routes.MyApiDetailsController.onPageLoad(apiDetail.id).url)
      val result = route(fixture.application, request).value

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

  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = userModel)
      .overrides(bind[ApiHubService].toInstance(apiHubService))
      .build()

    Fixture(apiHubService, application)
  }

}
