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

package controllers.application

import base.SpecBase
import controllers.actions.{FakeApiDetail, FakeApplication, FakeUser}
import controllers.routes
import play.api.i18n.{Messages, MessagesProvider}
import forms.AddCredentialChecklistFormProvider
import io.swagger.v3.oas.models.servers.Server
import models.application.*
import models.user.UserModel
import models.{CurlCommand, MDTP}
import org.mockito.ArgumentMatchers.{any, anyString, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.i18n.MessagesProvider
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{ApiHubService, CurlCommandService}
import utils.TestHelpers

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class CurlCommandControllerSpec extends SpecBase with MockitoSugar with TestHelpers {

  import CurlCommandControllerSpec.*

  private implicit val messagesProvider: MessagesProvider = mock[MessagesProvider]
  private val messages: Messages = mock[Messages]
  private val errorMessage = "Error message"
  when(messagesProvider.messages).thenReturn(messages)
  when(messages.apply(anyString, any)).thenReturn(errorMessage)

  "CurlCommandController.buildCurlCommand" - {
    "must return OK and the correct JSON for a GET from a support user when the application has no APIs" in {
      forAll(usersWhoCanSupport) {
        user =>
          val application = FakeApplication.copy(apis = Seq.empty)
          val fixture = buildFixture(user, application)

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.CurlCommandController.buildCurlCommand(FakeApplication.id, MDTP).url)
            val result = route(fixture.playApplication, request).value

            status(result) mustEqual OK
            contentAsJson(result) mustEqual Json.arr()
          }
      }
    }

    "must return OK and the correct JSON for a GET from a support user when the curl commands are generated" in {
      forAll(usersWhoCanSupport) {
        user =>
          val api1 = Api("api1", "API 1", Seq(SelectedEndpoint("GET", "/endpoint1")))
          val api2 = Api("api2", "API 2", Seq(SelectedEndpoint("POST", "/endpoint2")))
          val apiDetail1 = FakeApiDetail.copy(id=api1.id)
          val apiDetail2 = FakeApiDetail.copy(id=api2.id)
          val server = Server().url("http://example.com")
          val curl1 = CurlCommand("GET", Some(server), "/endpoint1", Map.empty, Map.empty, Map.empty, None)
          val curl2 = CurlCommand("GET", Some(server), "/endpoint2", Map.empty, Map.empty, Map.empty, None)
          val application = FakeApplication.copy(apis = Seq(api1, api2))
          val fixture = buildFixture(user, application)

          when(fixture.apiHubService.getApiDetail(eqTo(api1.id))(any())).thenReturn(Future.successful(Some(apiDetail1)))
          when(fixture.apiHubService.getApiDetail(eqTo(api2.id))(any())).thenReturn(Future.successful(Some(apiDetail2)))
          when(fixture.curlCommandService.buildCurlCommandsForApi(any(), eqTo(apiDetail1), any())(any())).thenReturn(Right(Seq(curl1)))
          when(fixture.curlCommandService.buildCurlCommandsForApi(any(), eqTo(apiDetail2), any())(any())).thenReturn(Right(Seq(curl2)))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.CurlCommandController.buildCurlCommand(FakeApplication.id, MDTP).url)
            val result = route(fixture.playApplication, request).value

            status(result) mustEqual OK
            contentAsJson(result) mustEqual Json.arr("curl -X 'GET' 'http://example.com/endpoint1'","curl -X 'GET' 'http://example.com/endpoint2'")
          }
      }
    }

    "must return Internal Server Error for a GET from a support user when none of the curl commands are generated" in {
      forAll(usersWhoCanSupport) {
        user =>
          val api = Api("api1", "API 1", Seq(SelectedEndpoint("GET", "/endpoint1")))
          val apiDetail = FakeApiDetail.copy(id = api.id)
          val application = FakeApplication.copy(apis = Seq(api))
          val fixture = buildFixture(user, application)

          when(fixture.apiHubService.getApiDetail(eqTo(api.id))(any())).thenReturn(Future.successful(Some(apiDetail)))
          when(fixture.curlCommandService.buildCurlCommandsForApi(any(), eqTo(apiDetail), any())(any())).thenReturn(Left("nope"))

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.CurlCommandController.buildCurlCommand(FakeApplication.id, MDTP).url)
            val result = route(fixture.playApplication, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR
          }
      }
    }

    "must redirect to Unauthorized error page for non-support users" in {
      forAll(usersWhoCannotSupport) {
        user =>
          val application = FakeApplication.copy(apis = Seq.empty)
          val fixture = buildFixture(user, application)

          running(fixture.playApplication) {
            val request = FakeRequest(GET, controllers.application.routes.CurlCommandController.buildCurlCommand(FakeApplication.id, MDTP).url)
            val result = route(fixture.playApplication, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
      }
    }

  }
}

object CurlCommandControllerSpec extends SpecBase with MockitoSugar {

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService, curlCommandService: CurlCommandService)

  private val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val formProvider = new AddCredentialChecklistFormProvider()
  private val form = formProvider()

  private def buildFixture(userModel: UserModel = FakeUser, application: Application): Fixture = {
    val apiHubService = mock[ApiHubService]
    val curlCommandService = mock[CurlCommandService]


    when(apiHubService.getApplication(any(), any(), any())(any()))
      .thenReturn(Future.successful(Some(application)))


    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[CurlCommandService].toInstance(curlCommandService)
      ).build()

    Fixture(playApplication, apiHubService, curlCommandService)
  }

}
