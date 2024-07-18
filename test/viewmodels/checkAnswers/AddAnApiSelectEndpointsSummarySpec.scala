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

package viewmodels.checkAnswers

import controllers.actions.{FakeApplication, FakeUser}
import controllers.routes
import models.api.ApiDetailLensesSpec.sampleOas
import models.api.{ApiDetail, Endpoint, EndpointMethod, Live}
import models.{AddAnApi, CheckMode, UserAnswers}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.AddAnApiSelectEndpointsPage
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

import java.time.Instant

class AddAnApiSelectEndpointsSummarySpec extends AnyFreeSpec with Matchers with OptionValues {

  private implicit val messages: Messages = play.api.test.Helpers.stubMessages()

  private val apiDetail: ApiDetail = ApiDetail(
    "test-id",
    "test-pub-ref",
    "test-title",
    "test-description",
    "test-version",
    Seq(
      Endpoint(
        "/path1",
        Seq(
          EndpointMethod("GET", None, None, Seq("test-scope-1"))
        )
      ),
      Endpoint(
        "/path2",
        Seq(
          EndpointMethod("POST", None, None, Seq("test-scope-1")),
          EndpointMethod("GET", None, None, Seq("test-scope-2"))
        )
      )
    ),
    None,
    sampleOas,
    Live,
    reviewedDate = Instant.now()
  )

  "row" - {
    "must return the correct summary list row when the question has an answer" in {
      val userAnswers = UserAnswers(FakeUser.userId, Json.obj(), Instant.now())
        .set(AddAnApiSelectEndpointsPage, Set(Set("test-scope-1")))
        .toOption
        .value

      val actual = AddAnApiSelectEndpointsSummary.row(
        userAnswers,
        apiDetail,
        FakeApplication,
        AddAnApi
      )

      val endpoint1 = "<li class='govuk-!-margin-bottom-3'><strong class='govuk-tag govuk-tag--blue govuk-!-margin-right-1'>GET</strong><code class='code--header govuk-!-margin-top-2'><strong class='bold-xsmall'>/path1</strong></code></li>"
      val endpoint2 = "<li class='govuk-!-margin-bottom-3'><strong class='govuk-tag govuk-tag--blue govuk-!-margin-right-1'>POST</strong><code class='code--header govuk-!-margin-top-2'><strong class='bold-xsmall'>/path2</strong></code></li>"
      val text = s"<ul class='govuk-list govuk-list--bullet'>$endpoint1$endpoint2</ul>"

      val expected = SummaryListRow(
        key = Key(Text("addAnApiSelectEndpoints.checkYourAnswersLabel")),
        value = Value(HtmlContent(text)),
        actions = Some(Actions(
          items = Seq(
            ActionItem(
              href = routes.AddAnApiSelectEndpointsController.onPageLoad(CheckMode, AddAnApi).url,
              content = Text("site.change"),
              visuallyHiddenText = Some("addAnApiSelectEndpoints.change.hidden")
            )
          )
        ))
      )

      actual mustBe Some(expected)
    }

    "must return the correct summary list when the question has not been answered" in {
      val actual = AddAnApiSelectEndpointsSummary.row(
        UserAnswers(FakeUser.userId, Json.obj(), Instant.now()),
        apiDetail,
        FakeApplication,
        AddAnApi
      )

      actual mustBe None
    }
  }

}
