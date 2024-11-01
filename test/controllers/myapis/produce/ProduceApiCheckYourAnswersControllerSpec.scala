/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.myapis.produce

import base.SpecBase
import controllers.actions.FakeUser
import controllers.routes
import forms.myapis.produce.ProduceApiReviewNameDescriptionFormProvider
import models.UserAnswers
import models.team.Team
import models.myapis.produce._
import models.myapis.produce.ProduceApiReviewNameDescription.Confirm
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.*
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import viewmodels.checkAnswers.myapis.produce._
import views.html.myapis.produce.{ProduceApiCheckYourAnswersView, ProduceApiReviewNameDescriptionView}
import viewmodels.govuk.all.SummaryListViewModel
import java.time.LocalDateTime
import scala.concurrent.Future
import models.api.Alpha
import play.api.i18n.Messages
import fakes.{FakeDomains, FakeHods}

class ProduceApiCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  lazy val produceApiCheckYourAnswersRoute = controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad().url

  val fullyPopulatedUserAnswers = UserAnswers(userAnswersId)
    .set(ProduceApiChooseTeamPage, Team("id", "name", LocalDateTime.now(), Seq.empty)).success.value
    .set(ProduceApiEnterOasPage, "oas").success.value
    .set(ProduceApiEnterApiTitlePage, "api name").success.value
    .set(ProduceApiShortDescriptionPage, "api description").success.value
    .set(ProduceApiChooseEgressPage, ProduceApiChooseEgress(Some("egress"), true)).success.value
    .set(ProduceApiEgressPrefixesPage, ProduceApiEgressPrefixes(Seq("/prefix"), Seq("/existing->/replacement"))).success.value
    .set(ProduceApiHodPage, Set("hod1")).success.value
    .set(ProduceApiDomainPage, ProduceApiDomainSubdomain("domain", "subdomain")).success.value
    .set(ProduceApiStatusPage, Alpha).success.value
    .set(ProduceApiPassthroughPage, true).success.value

  "ProduceApiCheckYourAnswersController" - {
    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      implicit val msgs: Messages = messages(fixture.application)
      
      running(fixture.application) {
        val request = FakeRequest(GET, produceApiCheckYourAnswersRoute)
        val result = route(fixture.application, request).value
        val view = fixture.application.injector.instanceOf[ProduceApiCheckYourAnswersView]
        val expectedSummaryList = SummaryListViewModel(Seq(
          ProduceApiChooseTeamSummary.row(fullyPopulatedUserAnswers),
          ProduceApiEnterOasSummary.row(fullyPopulatedUserAnswers),
          ProduceApiNameSummary.row(fullyPopulatedUserAnswers),
          ProduceApiShortDescriptionSummary.row(fullyPopulatedUserAnswers),
          ProduceApiEgressSummary.row(fullyPopulatedUserAnswers),
          ProduceApiEgressPrefixesSummary.row(fullyPopulatedUserAnswers),
          ProduceApiHodSummary.row(fullyPopulatedUserAnswers, FakeHods),
          ProduceApiDomainSummary.row(fullyPopulatedUserAnswers, FakeDomains),
          ProduceApiSubDomainSummary.row(fullyPopulatedUserAnswers, FakeDomains),
          ProduceApiStatusSummary.row(fullyPopulatedUserAnswers),
          ProduceApiPassthroughSummary.row(fullyPopulatedUserAnswers)
        ).flatten)
        
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(expectedSummaryList, FakeUser)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiCheckYourAnswersRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }

  private case class Fixture(application: PlayApplication)

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val playApplication = applicationBuilder(userAnswers).build()
    Fixture(playApplication)
  }
}
