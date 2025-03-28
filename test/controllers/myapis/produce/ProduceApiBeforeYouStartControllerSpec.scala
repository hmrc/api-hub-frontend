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
import navigation.{FakeNavigator, Navigator}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.HtmlValidation
import viewmodels.RelatedContentLink
import viewmodels.myapis.produce.ProduceApiBeforeYouStartViewModel
import views.html.myapis.produce.ProduceApiBeforeYouStartView

class ProduceApiBeforeYouStartControllerSpec extends SpecBase with HtmlValidation {

  import ProduceApiBeforeYouStartControllerSpec.*

  private val nextPage = controllers.routes.IndexController.onPageLoad

  "ProduceApiBeforeYouStart Controller" - {
    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture()

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.produce.routes.ProduceApiBeforeYouStartController.onPageLoad())
        val result = route(fixture.application, request).value
        val viewModel = ProduceApiBeforeYouStartViewModel(
          nextPage.url,
          relatedContentLinks,
          "produceApiBeforeYouStart.heading",
          "produceApiBeforeYouStart.beforeYouStart.content",
          "produceApiBeforeYouStart.creationProcess.heading",
          "produceApiBeforeYouStart.creationProcess.content",
          Seq(
            "produceApiBeforeYouStart.creationProcess.list.1",
            "produceApiBeforeYouStart.creationProcess.list.2",
            "produceApiBeforeYouStart.creationProcess.list.3",
            "produceApiBeforeYouStart.creationProcess.list.4",
          )
        )
        val view = fixture.application.injector.instanceOf[ProduceApiBeforeYouStartView]
        val nextPageUrl = nextPage.url

        status(result) mustEqual OK
        contentAsString(result) mustBe view(FakeUser, viewModel)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(application: Application)

  private def buildFixture(): Fixture = {
    val application = applicationBuilder(Some(emptyUserAnswers))
      .overrides(
        bind[Navigator].toInstance(new FakeNavigator(nextPage))
      )
      .build()

    Fixture(application)
  }

}

object ProduceApiBeforeYouStartControllerSpec {

  private val relatedContentLinks: Seq[RelatedContentLink] = Seq(
    RelatedContentLink(
      "Producing APIs",
      "http://localhost:8490/guides/integration-hub-guide/documentation/how-do-i-produce.apis.html#how-do-i-produce-apis"
    ),
    RelatedContentLink(
      "Consuming APIs",
      "http://localhost:8490/guides/integration-hub-guide/documentation/how-do-I-consume-apis.html#how-do-i-consume-apis"
    )
  )

}
