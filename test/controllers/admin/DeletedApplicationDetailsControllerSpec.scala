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

package controllers.admin

import base.SpecBase
import controllers.actions.{FakeApplication, FakeUser}
import controllers.routes
import generators.AccessRequestGenerator
import models.application._
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.ErrorTemplate
import views.html.application.DeletedApplicationDetailsView

import java.time.LocalDateTime
import scala.concurrent.Future

class DeletedApplicationDetailsControllerSpec extends SpecBase with MockitoSugar with TestHelpers with AccessRequestGenerator with HtmlValidation{

  "DeletedApplicationDetailsController Controller" - {
    "must return OK and the correct view for a GET for a support user" in {
      forAll(usersWhoCanSupport) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)
          val deletedApplication = FakeApplication.copy(deleted = Some(Deleted(LocalDateTime.now, "delete@example.com")))
          val accessRequests = Seq(sampleAccessRequest())

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(deletedApplication.id), ArgumentMatchers.eq(false), ArgumentMatchers.eq(true))(any()))
            .thenReturn(Future.successful(Some(deletedApplication)))

          when(fixture.apiHubService.getAccessRequests(ArgumentMatchers.eq(Some(deletedApplication.id)), ArgumentMatchers.eq(None))(any()))
            .thenReturn(Future.successful(accessRequests))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.admin.routes.DeletedApplicationDetailsController.onPageLoad(deletedApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[DeletedApplicationDetailsView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(deletedApplication, accessRequests, Some(user))(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must sort the access requests, most recent first" in {
      forAll(usersWhoCanSupport) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)
          val deletedApplication = FakeApplication.copy(deleted = Some(Deleted(LocalDateTime.now, "delete@example.com")))
          val unsortedAccessRequests = Seq(sampleAccessRequest())
          val sortedAccessRequests = unsortedAccessRequests.sortBy(_.requested).reverse

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(deletedApplication.id), ArgumentMatchers.eq(false), ArgumentMatchers.eq(true))(any()))
            .thenReturn(Future.successful(Some(deletedApplication)))

          when(fixture.apiHubService.getAccessRequests(ArgumentMatchers.eq(Some(deletedApplication.id)), ArgumentMatchers.eq(None))(any()))
            .thenReturn(Future.successful(unsortedAccessRequests))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.admin.routes.DeletedApplicationDetailsController.onPageLoad(deletedApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[DeletedApplicationDetailsView]

            status(result) mustEqual OK
            contentAsString(result) mustBe view(deletedApplication, sortedAccessRequests, Some(user))(request, messages(fixture.playApplication)).toString
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must return 404 Not Found when the application does not exist" in {
      forAll(usersWhoCanSupport) {
        user: UserModel =>
          val fixture = buildFixture(user)

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), any(), any())(any()))
            .thenReturn(Future.successful(None))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.admin.routes.DeletedApplicationDetailsController.onPageLoad(FakeApplication.id).url)
            val result = route(fixture.playApplication, request).value
            val view = fixture.playApplication.injector.instanceOf[ErrorTemplate]

            status(result) mustBe NOT_FOUND
            contentAsString(result) mustBe
              view(
                "Page not found - 404",
                "Application not found",
                s"Cannot find an application with Id ${FakeApplication.id}."
              )(request, messages(fixture.playApplication))
                .toString()
            contentAsString(result) must validateAsHtml
          }
      }
    }

    "must redirect to Unauthorised page for a GET when user is not a supporter" in {
      forAll(usersWhoCannotSupport) {
        user: UserModel =>
          val fixture = buildFixture(userModel = user)

          when(fixture.apiHubService.getApplication(ArgumentMatchers.eq(FakeApplication.id), ArgumentMatchers.eq(false), ArgumentMatchers.eq(true))(any()))
            .thenReturn(Future.successful(Some(FakeApplication)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.admin.routes.DeletedApplicationDetailsController.onPageLoad(FakeApplication.id).url)
            val result = route(fixture.playApplication, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
          }
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
