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
import models.requests.{ApplicationRequest, IdentifierRequest}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import views.html.ErrorTemplate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationAuthActionSpec extends SpecBase with Matchers with MockitoSugar {

  "ApplicationAuthAction" - {
    "must grant a user access to an application when they are in the team" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      val result = fixture.provider.apply(FakeApplication.id).invokeBlock(buildRequest(), buildInvokeBlock())
      status(result) mustBe OK
    }

    "must grant a user access to an application when they are a supporter but not in the team" in {
      val fixture = buildFixture()
      val application = FakeApplication.copy(teamMembers = Seq.empty)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      val result = fixture.provider.apply(application.id).invokeBlock(buildRequest(FakeSupporter), buildInvokeBlock())
      status(result) mustBe OK
    }

    "must redirect to the Unauthorised page when the user is not in the team or an supporter" in {
      val fixture = buildFixture()
      val application = FakeApplication.copy(teamMembers = Seq.empty)

      when(fixture.apiHubService.getApplication(eqTo(application.id), eqTo(false), eqTo(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      val result = fixture.provider.apply(application.id).invokeBlock(buildRequest(), buildInvokeBlock())
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }

    "must return Not Found with a suitable message when the application does not exist" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(eqTo(FakeApplication.id), eqTo(false), eqTo(false))(any()))
        .thenReturn(Future.successful(None))

      running(fixture.playApplication) {
        val result = fixture.provider.apply(FakeApplication.id).invokeBlock(buildRequest(), buildInvokeBlock())

        status(result) mustBe NOT_FOUND

        val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Application not found",
            s"Cannot find an application with ID ${FakeApplication.id}.",
            Some(FakeUser)
          )(FakeRequest(), messages(fixture.playApplication))
            .toString()
      }
    }
  }

  case class Fixture(
    provider: ApplicationAuthActionProvider,
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

    val provider = playApplication.injector.instanceOf[ApplicationAuthActionProvider]
    Fixture(provider, apiHubService, playApplication)
  }

  def buildInvokeBlock[A](): ApplicationRequest[A] => Future[Result] = {
    _ => Future.successful(Results.Ok)
  }

  def buildRequest(user: UserModel = FakeUser): IdentifierRequest[AnyContentAsEmpty.type] = {
    IdentifierRequest(FakeRequest(), user)
  }

}
