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

import models.requests.{ApplicationRequest, IdentifierRequest}
import models.user.UserModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService

import scala.concurrent.Future

class ApplicationAuthActionSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  import ApplicationAuthActionSpec._

  "ApplicationAuthAction" - {
    "must grant a user access to an application when they are in the team" in {
      val fixture = buildFixture()

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      fixture.provider.apply(FakeApplication.id).invokeBlock(buildRequest(), buildInvokeBlock()).map {
        result =>
          result.header.status mustBe OK
      }
    }

    "must grant a user access to an application when they are an administrator but not in the team" in {
      val fixture = buildFixture()
      val application = FakeApplication.copy(teamMembers = Seq.empty)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      fixture.provider.apply(application.id).invokeBlock(buildRequest(FakeAdministrator), buildInvokeBlock()).map {
        result =>
          result.header.status mustBe OK
      }
    }

    "must redirect to the Unauthorised page when the user is not in the team or an administrator" in {
      val fixture = buildFixture()
      val application = FakeApplication.copy(teamMembers = Seq.empty)

      when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(application.id), ArgumentMatchers.eq(false))(any()))
        .thenReturn(Future.successful(Some(application)))

      fixture.provider.apply(application.id).invokeBlock(buildRequest(), buildInvokeBlock()).map {
        result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get(LOCATION) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

}

object ApplicationAuthActionSpec extends MockitoSugar {

  case class Fixture(provider: ApplicationAuthActionProvider, apiHubService: ApiHubService)

  def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]
    Fixture(new ApplicationAuthActionProviderImpl(apiHubService), apiHubService)
  }

  def buildInvokeBlock[A](): ApplicationRequest[A] => Future[Result] = {
    _ => Future.successful(Results.Ok)
  }

  def buildRequest(user: UserModel = FakeUser): IdentifierRequest[AnyContentAsEmpty.type] = {
    IdentifierRequest(FakeRequest(), user)
  }

}
