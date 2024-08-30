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

package controllers

import base.SpecBase
import controllers.ApplicationsControllerSpec.buildFixture
import controllers.actions.FakeUser
import models.application.{Application, Creator, TeamMember}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ApplicationsView

import scala.concurrent.Future

class ApplicationsControllerSpec extends SpecBase with MockitoSugar with HtmlValidation {

  "Applications Controller" - {

    "must return OK and the correct view for a GET" in {

      val testEmail = "test-email"
      val creatorEmail = "creator-email-2"
      val applications = Seq(
        Application("id-1", "app-name-1", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail))),
        Application("id-2", "app-name-2", Creator(creatorEmail), Seq.empty).copy(teamMembers = Seq(TeamMember(testEmail)))
      )

      val fixture = buildFixture()

      running(fixture.application) {

        when(fixture.mockApiHubService.getApplications(eqTo(Some(testEmail)), eqTo(false))(any()))
          .thenReturn(Future.successful(applications))

        val request = FakeRequest(GET, routes.ApplicationsController.onPageLoad.url)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ApplicationsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(applications, Some(FakeUser))(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }

}

object ApplicationsControllerSpec extends SpecBase with MockitoSugar {

  case class Fixture(
    application: PlayApplication,
    mockSessionRepository: SessionRepository,
    mockApiHubService: ApiHubService
  )

  def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val apiHubService = mock[ApiHubService]
    val mockSessionRepository = mock[SessionRepository]
    val application = applicationBuilder(userAnswers = None, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, mockSessionRepository, apiHubService)
  }

}
