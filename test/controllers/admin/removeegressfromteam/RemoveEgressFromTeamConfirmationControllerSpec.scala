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

package controllers.admin.removeegressfromteam

import base.SpecBase
import controllers.actions.FakeSupporter
import fakes.FakeTeam
import models.api.EgressGateway
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import viewmodels.admin.removeegressfromteam.{RemoveEgressFromTeamConfirmationViewModel, RemoveEgressFromTeamSuccessViewModel}
import views.html.ErrorTemplate
import views.html.admin.removeegressfromteam.{RemoveEgressFromTeamConfirmationView, RemoveEgressFromTeamSuccessView}

import scala.concurrent.Future

class RemoveEgressFromTeamConfirmationControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with TestHelpers {

  import RemoveEgressFromTeamConfirmationControllerSpec.*

  "onPageLoad" - {
    "must return Ok and the correct view for a support user" in {
      forAll(usersWhoCanSupport) {user =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.findTeamById(eqTo(FakeTeam.id))(any)).thenReturn(Future.successful(Some(FakeTeam)))
        when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(Seq(egress)))

        running(fixture.application) {
          val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onPageLoad(FakeTeam.id, egress.id))
          val result = route(fixture.application, request).value

          status(result) mustBe OK

          val view = fixture.application.injector.instanceOf[RemoveEgressFromTeamConfirmationView]
          val viewModel = RemoveEgressFromTeamConfirmationViewModel(FakeTeam, egress, user)

          contentAsString(result) mustBe view(viewModel)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must redirect to the unauthorised page for non-support users" in {
      forAll(usersWhoCannotSupport) {user =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onPageLoad(FakeTeam.id, egress.id))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "must display a Not Found page when the team does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.findTeamById(eqTo(FakeTeam.id))(any)).thenReturn(Future.successful(None))
      when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(Seq(egress)))

      running(fixture.application) {
        val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onPageLoad(FakeTeam.id, egress.id))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe view(
          "Page not found - 404",
          "Team not found",
          s"Cannot find a team with ID ${FakeTeam.id}.",
          Some(FakeSupporter)
        )(request, messages(fixture.application)).toString

        contentAsString(result) must validateAsHtml
      }
    }

    "must display a Not Found page when the egress does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.findTeamById(eqTo(FakeTeam.id))(any)).thenReturn(Future.successful(Some(FakeTeam)))
      when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(Seq.empty))

      running(fixture.application) {
        val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onPageLoad(FakeTeam.id, egress.id))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe view(
          "Page not found - 404",
          "Egress not found",
          s"Cannot find an egress with ID ${egress.id}.",
          Some(FakeSupporter)
        )(request, messages(fixture.application)).toString

        contentAsString(result) must validateAsHtml
      }
    }
  }

  "onSubmit" - {
    "must remove the egress and return the success view for a support user" in {
      forAll(usersWhoCanSupport) {user =>
        val fixture = buildFixture(user)

        when(fixture.apiHubService.findTeamById(eqTo(FakeTeam.id))(any)).thenReturn(Future.successful(Some(FakeTeam)))
        when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(Seq(egress)))

        when(fixture.apiHubService.removeEgressFromTeam(any, any)(any)).thenReturn(Future.successful(Some(())))

        running(fixture.application) {
          val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onSubmit(FakeTeam.id, egress.id))
          val result = route(fixture.application, request).value

          status(result) mustBe OK

          val view = fixture.application.injector.instanceOf[RemoveEgressFromTeamSuccessView]
          val viewModel = RemoveEgressFromTeamSuccessViewModel(FakeTeam, user)

          contentAsString(result) mustBe view(viewModel)(request, messages(fixture.application)).toString
          contentAsString(result) must validateAsHtml

          verify(fixture.apiHubService).removeEgressFromTeam(eqTo(FakeTeam.id), eqTo(egress.id))(any)
        }
      }
    }

    "must redirect to the unauthorised page for non-support users" in {
      forAll(usersWhoCannotSupport) {user =>
        val fixture = buildFixture(user)

        running(fixture.application) {
          val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onSubmit(FakeTeam.id, egress.id))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "must display a Not Found page when the team does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.findTeamById(eqTo(FakeTeam.id))(any)).thenReturn(Future.successful(None))
      when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(Seq(egress)))

      running(fixture.application) {
        val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onSubmit(FakeTeam.id, egress.id))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe view(
          "Page not found - 404",
          "Team not found",
          s"Cannot find a team with ID ${FakeTeam.id}.",
          Some(FakeSupporter)
        )(request, messages(fixture.application)).toString

        contentAsString(result) must validateAsHtml
      }
    }

    "must display a Not Found page when the egress does not exist" in {
      val fixture = buildFixture(FakeSupporter)

      when(fixture.apiHubService.findTeamById(eqTo(FakeTeam.id))(any)).thenReturn(Future.successful(Some(FakeTeam)))
      when(fixture.apiHubService.listEgressGateways(any)(any)).thenReturn(Future.successful(Seq.empty))

      running(fixture.application) {
        val request = FakeRequest(routes.RemoveEgressFromTeamConfirmationController.onSubmit(FakeTeam.id, egress.id))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        val view = fixture.application.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe view(
          "Page not found - 404",
          "Egress not found",
          s"Cannot find an egress with ID ${egress.id}.",
          Some(FakeSupporter)
        )(request, messages(fixture.application)).toString

        contentAsString(result) must validateAsHtml
      }
    }
  }

  private def buildFixture(user: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(user = user)
      .overrides(bind[ApiHubService].toInstance(apiHubService))
      .build()

    Fixture(application, apiHubService)
  }

}

private object RemoveEgressFromTeamConfirmationControllerSpec {

  case class Fixture(
    application: Application,
    apiHubService: ApiHubService
  )

  val egress: EgressGateway = EgressGateway(
    id = "test-egress-id",
    friendlyName = "test-friendly-name"
  )

}
