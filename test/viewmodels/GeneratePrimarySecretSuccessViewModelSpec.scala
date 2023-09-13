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

package viewmodels

import config.EnvironmentNames
import controllers.actions.FakeApplication
import models.application.ApplicationLenses.ApplicationLensOps
import models.application._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import viewmodels.GeneratePrimarySecretSuccessViewModelSpec._

class GeneratePrimarySecretSuccessViewModelSpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = play.api.test.Helpers.stubMessages()

  "GeneratePrimarySecretSuccessViewModel.buildSummary" - {
    "must construct the correct view model when scopes are present" in {
      val scope1 = Scope("test-scope-1", Approved)
      val scope2 = Scope("test-scope-2", Approved)
      val scope3 = Scope("test-scope-3", Pending)

      val application = FakeApplication
        .setPrimaryCredentials(Seq(credential))
        .setPrimaryScopes(Seq(scope1, scope2, scope3))

      val expected = buildSummaryList(application, s"${scope1.name}, ${scope2.name}")

      val actual = GeneratePrimarySecretSuccessViewModel.buildSummary(application, environmentNames, credential, secret)

      actual mustBe expected
    }

    "must construct the correct view model when there are no scopes" in {
      val application = FakeApplication
        .setPrimaryCredentials(Seq(credential))
        .setPrimaryScopes(Seq.empty)

      val expected = buildSummaryList(application, "")

      val actual = GeneratePrimarySecretSuccessViewModel.buildSummary(application, environmentNames, credential, secret)

      actual mustBe expected
    }
  }

}

object GeneratePrimarySecretSuccessViewModelSpec {

  private val environmentNames = EnvironmentNames("primary", "secondary")
  private val credential = Credential("test-client-id", None, None)
  private val secret = Secret("test-secret")

  def buildSummaryList(application: Application, scopes: String)(implicit messages: Messages): SummaryList = {
    SummaryList(
      Seq(
        SummaryListRow(
          key = Key(Text(messages("generatePrimarySecretSuccess.applicationName"))),
          value = Value(Text(application.name)),
          actions = Some(Actions())
        ),
        SummaryListRow(
          key = Key(Text(messages("generatePrimarySecretSuccess.environment"))),
          value = Value(Text("primary")),
          actions = Some(Actions())
        ),
        SummaryListRow(
          key = Key(Text(messages("generatePrimarySecretSuccess.scopes"))),
          value = Value(Text(scopes)),
          actions = Some(Actions())
        ),
        SummaryListRow(
          key = Key(Text(messages("generatePrimarySecretSuccess.clientId"))),
          value = Value(Text(credential.clientId)),
          actions = Some(Actions())
        ),
        SummaryListRow(
          key = Key(Text(messages("generatePrimarySecretSuccess.clientSecret"))),
          value = Value(Text(secret.secret)),
          actions = Some(Actions(items = Seq(
            ActionItem(
              href = "#",
              content = Text("Copy"),
              classes = "govuk-button",
              attributes = Map("id" -> "copy-to-clipboard-button")
            )
          )))
        )
      ))
  }

}
