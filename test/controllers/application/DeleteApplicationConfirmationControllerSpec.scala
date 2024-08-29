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
import controllers.actions.{FakeApplication, FakeUser}
import forms.ConfirmationFormProvider
import generators.ApiDetailGenerators
import models.application.Application
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList, SummaryListRow}
import utils.HtmlValidation
import views.html.application.{DeleteApplicationConfirmationView, DeleteApplicationSuccessView}

import scala.concurrent.Future

class DeleteApplicationConfirmationControllerSpec extends SpecBase with MockitoSugar with HtmlValidation with ApiDetailGenerators {

  private val form = new ConfirmationFormProvider()("deleteApplicationConfirmation.error")

  "GET" - {
    "must return OK and the confirmation view" in {
      val fixture = buildFixture

      when(fixture.apiHubService.getApplication(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          GET,
          controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(FakeApplication.id).url
        )
        implicit val msgs: Messages = messages(fixture.application)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[DeleteApplicationConfirmationView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeApplication.id, form, buildSummaryList(FakeApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  "POST" - {
    "must return OK and the success view when the application has been successfully deleted" in {
      val fixture = buildFixture

      when(fixture.apiHubService.getApplication(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      when(fixture.apiHubService.deleteApplication(any(), any())(any()))
        .thenReturn(Future.successful(Some(())))

      running(fixture.application) {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(
          POST,
          controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(FakeApplication.id).url
        ).withFormUrlEncodedBody(("value[0]", "confirm"))
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[DeleteApplicationSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeUser)(request, messages(fixture.application)).toString()
        contentAsString(result) must validateAsHtml
        verify(fixture.apiHubService).deleteApplication(eqTo(FakeApplication.id), eqTo(FakeUser.email))(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val fixture = buildFixture

      when(fixture.apiHubService.getApplication(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(FakeApplication)))

      running(fixture.application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          POST,
          controllers.application.routes.DeleteApplicationConfirmationController.onPageLoad(FakeApplication.id).url
        )
        implicit val msgs: Messages = messages(fixture.application)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[DeleteApplicationConfirmationView]
        val formWithErrors = form.bind(Map.empty[String, String])

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(FakeApplication.id, formWithErrors, buildSummaryList(FakeApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(
    application: PlayApplication,
    apiHubService: ApiHubService
  )

  private def buildFixture: Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(None)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(application, apiHubService)
  }

  private def buildSummaryList(application: Application): SummaryList = {
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(Text("Name")),
          value = Value(Text(application.name))
        )
      )
    )
  }

}
