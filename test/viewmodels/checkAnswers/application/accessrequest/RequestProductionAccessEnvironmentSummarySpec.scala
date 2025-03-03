/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.checkAnswers.application.accessrequest

import config.HipEnvironment
import fakes.FakeHipEnvironments
import models.UserAnswers
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.application.accessrequest.RequestProductionAccessEnvironmentIdPage
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Key, SummaryListRow, Value}

class RequestProductionAccessEnvironmentSummarySpec extends AnyFreeSpec with Matchers with OptionValues {

  import RequestProductionAccessEnvironmentSummarySpec.*

  "RequestProductionAccessEnvironmentSummary" - {
    "must provide the correct summary when the answer exists" in {
      val userAnswers = UserAnswers("test-id").set(
        RequestProductionAccessEnvironmentIdPage,
        FakeHipEnvironments.production.id
      ).toOption.value
      val actual = environmentSummary.row(userAnswers)
      val expected = buildSummary(Some(FakeHipEnvironments.production))

      actual mustBe expected
    }

    "must provide the correct summary when there is no answer" in {
      val userAnswers = UserAnswers("test-id")
      val actual = environmentSummary.row(userAnswers)
      val expected = buildSummary(None)

      actual mustBe expected
    }


  }

}

object RequestProductionAccessEnvironmentSummarySpec {

  private implicit val messages: Messages = Helpers.stubMessages()

  private val environmentSummary = new RequestProductionAccessEnvironmentSummary(FakeHipEnvironments)

  private def buildSummary(environment: Option[HipEnvironment]): SummaryListRow = {
    val value = environment
      .map(environment => Value(Text(environment.nameKey)))
      .getOrElse(Value())

    SummaryListRow(
      key = Key(Text("requestProductionAccess.environment.name")),
      value = value,
      actions = Some(
        Actions(
          items = Seq.empty
        )
      )
    )
  }

}
