/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.myapis.promote

import base.SpecBase
import controllers.actions.{FakeApiDetail, FakeUser}
import fakes.FakeHipEnvironments
import forms.myapis.produce.ProduceApiEgressSelectionForm
import generators.EgressGenerator
import models.api.ApiDeploymentStatus.Deployed
import models.api.ApiDeploymentStatuses
import models.deployment.{FailuresResponse, InvalidOasResponse, SuccessfulDeploymentsResponse}
import models.team.Team
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.myapis.promote.MyApiSetEgressViewModel
import views.html.ErrorTemplate
import views.html.myapis.promote.{MyApiPromoteSuccessView, MyApiSetEgressView}

import java.time.LocalDateTime
import scala.concurrent.Future

class MyApiSetEgressControllerSpec extends SpecBase with MockitoSugar with EgressGenerator with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = ProduceApiEgressSelectionForm()
  private val form = formProvider()
  private val deploymentStatuses = ApiDeploymentStatuses(Seq(
    Deployed(FakeHipEnvironments.production.id, "1.0"),
    Deployed(FakeHipEnvironments.test.id, "1.0")
  ))
  private val teamId = "teamId"
  private val teamName = "teamName"
  private val apiTeam = Team(teamId, teamName, LocalDateTime.now(), List.empty)

  "MyApiSetEgressController Controller" - {

    "must return OK and the correct view for a GET" in {
      val egressGateways = sampleEgressGateways()
      val fixture = buildFixture()
      val apiDetail = FakeApiDetail.copy(teamId = Some(apiTeam.id))
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.production))(any)).thenReturn(Future.successful(egressGateways))
      when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any)).thenReturn(Future.successful(List(apiTeam)))
      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any)).thenReturn(Future.successful(deploymentStatuses))

      running(fixture.application) {
        val url = controllers.myapis.promote.routes.MyApiSetEgressController.onPageLoad(apiDetail.id, FakeHipEnvironments.deployTo.id).url
        val request = FakeRequest(GET, url)

        val result = route(fixture.application, request).value

        val viewModel = MyApiSetEgressViewModel(
          apiDetail,
          FakeHipEnvironments.deployTo,
          FakeHipEnvironments.production,
          Some(FakeUser),
          egressGateways,
          deploymentStatuses
        )(messages(fixture.application))
        val view = fixture.application.injector.instanceOf[MyApiSetEgressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to Unauthorised page for a non-support user not on the api team" in {
      val fixture = buildFixture()
      val apiDetail = FakeApiDetail

      running(fixture.application) {
        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any)).thenReturn(Future.successful(Seq.empty))
        val url = controllers.myapis.promote.routes.MyApiSetEgressController.onPageLoad(apiDetail.id, FakeHipEnvironments.deployTo.id).url
        val request = FakeRequest(GET, url)
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "must return success view after form is submitted" in {
      val fixture = buildFixture()
      val apiDetail = FakeApiDetail
      val fromEnvironment = FakeHipEnvironments.deployTo
      val toEnvironment = fromEnvironment.promoteTo.get

      running(fixture.application) {
        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any)).thenReturn(Future.successful(List(apiTeam)))
        when(fixture.apiHubService.promoteAPI(eqTo(apiDetail.publisherReference), any, any, any)(any)).thenReturn(
          Future.successful(Some(SuccessfulDeploymentsResponse(apiDetail.id, "1.0", 1, ""))))
        val url = controllers.myapis.promote.routes.MyApiSetEgressController.onSubmit(apiDetail.id, fromEnvironment.id).url
        val request = FakeRequest(POST, url).withFormUrlEncodedBody("egress" -> "egressId")
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[MyApiPromoteSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          apiDetail,
          fromEnvironment,
          toEnvironment,
          FakeUser
        )(request, messages(fixture.application)).toString
      }
    }

    "must show error page if promotion fails" in {
      val fixture = buildFixture()
      val apiDetail = FakeApiDetail

      running(fixture.application) {
        when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
        when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any)).thenReturn(Future.successful(List(apiTeam)))
        when(fixture.apiHubService.promoteAPI(eqTo(apiDetail.publisherReference), any, any, any)(any)).thenReturn(
          Future.successful(Some(InvalidOasResponse(FailuresResponse("err", "err", None)))))
        val url = controllers.myapis.promote.routes.MyApiSetEgressController.onSubmit(apiDetail.id, FakeHipEnvironments.deployTo.id).url
        val request = FakeRequest(POST, url).withFormUrlEncodedBody("egress" -> "egressId")
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe view.apply(
            pageTitle = "Sorry, there is a problem with the service - 500",
            heading = "Sorry, there is a problem with the service",
            message = "Try again later.",
            Some(FakeUser)
          )(request, messages(fixture.application))
          .toString()
        contentAsString(result) must validateAsHtml
      }
    }

    "must return not found when the API is not Hub maintainable" in {
      val egressGateways = sampleEgressGateways()
      val fixture = buildFixture()
      val apiDetail = FakeApiDetail.copy(teamId = Some(apiTeam.id), apiGeneration = None)
      when(fixture.apiHubService.getApiDetail(eqTo(apiDetail.id))(any)).thenReturn(Future.successful(Some(apiDetail)))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.production))(any)).thenReturn(Future.successful(egressGateways))
      when(fixture.apiHubService.findTeams(eqTo(Some(FakeUser.email)))(any)).thenReturn(Future.successful(List(apiTeam)))
      when(fixture.apiHubService.getApiDeploymentStatuses(eqTo(apiDetail.publisherReference))(any)).thenReturn(Future.successful(deploymentStatuses))

      running(fixture.application) {
        val url = controllers.myapis.promote.routes.MyApiSetEgressController.onPageLoad(apiDetail.id, FakeHipEnvironments.deployTo.id).url
        val request = FakeRequest(GET, url)

        val result = route(fixture.application, request).value

        val viewModel = MyApiSetEgressViewModel(
          apiDetail,
          FakeHipEnvironments.deployTo,
          FakeHipEnvironments.production,
          Some(FakeUser),
          egressGateways,
          deploymentStatuses
        )(messages(fixture.application))
        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe view.apply(
            "Page not found - 404",
            "This page can’t be found",
            message = "This API is not maintainable by The Integration Hub",
            Some(FakeUser)
          )(request, messages(fixture.application))
          .toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(application: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
      )
      .build()

    Fixture(playApplication, apiHubService)
  }

}