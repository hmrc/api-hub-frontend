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

package controllers.actions

import base.SpecBase
import models.requests.{ApiRequest, IdentifierRequest}
import models.team.Team
import models.user.UserModel
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.ErrorTemplate

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiAuthActionSpec extends SpecBase with Matchers with MockitoSugar with ArgumentMatchersSugar {

  "ApiAuthAction" - {
    "if API does not exist then it returns a 404" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApiDetail(eqTo(FakeApiDetail.id))(any)).thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val result = fixture.provider.apply(FakeApiDetail.id).invokeBlock(buildRequest(), buildInvokeBlock())

        status(result) mustBe NOT_FOUND

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with Id ${FakeApiDetail.id}."
          )(FakeRequest(), messages(fixture.playApplication)).toString()
      }
    }

    "if API exists and user has the support role then it returns OK" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApiDetail(eqTo(FakeApiDetail.id))(any)).thenReturn(Future.successful(Some(FakeApiDetail)))

      running(fixture.playApplication) {
        val result = fixture.provider.apply(FakeApiDetail.id).invokeBlock(buildRequest(FakeSupporter), buildInvokeBlock())

        status(result) mustBe OK
        verify(fixture.apiHubService, never).findTeams(eqTo(FakeSupporter.email))(any)
      }
    }

    "if API exists but is not owned by any team then it returns Unauthorized" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApiDetail(eqTo(FakeApiDetail.id))(any)).thenReturn(
        Future.successful(Some(FakeApiDetail.copy(teamId = None)))
      )

      running(fixture.playApplication) {
        val result = fixture.provider.apply(FakeApiDetail.id).invokeBlock(buildRequest(FakeUser), buildInvokeBlock())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        verify(fixture.apiHubService, never).findTeams(eqTo(FakeSupporter.email))(any)
      }
    }

    "if API exists and has an owning team but the user is not in that team then it returns Unauthorized" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApiDetail(eqTo(FakeApiDetail.id))(any)).thenReturn(
        Future.successful(Some(FakeApiDetail))
      )
      when(fixture.apiHubService.findTeams(eqTo(FakeUser.email))(any)).thenReturn(
        Future.successful(Seq(Team(FakeApiDetail.teamId.get + "different", "team name", LocalDateTime.now(), Seq.empty)))
      )

      running(fixture.playApplication) {
        val result = fixture.provider.apply(FakeApiDetail.id).invokeBlock(buildRequest(FakeUser), buildInvokeBlock())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "if API exists and has an owning team and the user is in that team then it returns Ok" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApiDetail(eqTo(FakeApiDetail.id))(any)).thenReturn(
        Future.successful(Some(FakeApiDetail))
      )
      when(fixture.apiHubService.findTeams(eqTo(FakeUser.email))(any)).thenReturn(
        Future.successful(Seq(
          Team("id1", "team name 1", LocalDateTime.now(), Seq.empty),
          Team(FakeApiDetail.teamId.get, "team name 2", LocalDateTime.now(), Seq.empty),
          Team("id2", "team name 3", LocalDateTime.now(), Seq.empty)
        ))
      )

      running(fixture.playApplication) {
        val result = fixture.provider.apply(FakeApiDetail.id).invokeBlock(buildRequest(FakeUser), buildInvokeBlock())

        status(result) mustBe OK
      }
    }

  }

  case class Fixture(
    provider: ApiAuthActionProvider,
    apiHubService: ApiHubService,
    playApplication: Application
  )

  def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
      )
      .build()

    val provider = playApplication.injector.instanceOf[ApiAuthActionProvider]
    Fixture(provider, apiHubService, playApplication)
  }

  def buildInvokeBlock[A](): ApiRequest[A] => Future[Result] = {
    _ => Future.successful(Results.Ok)
  }

  def buildRequest(user: UserModel = FakeUser): IdentifierRequest[AnyContentAsEmpty.type] = {
    IdentifierRequest(FakeRequest(), user)
  }

}
