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
import models.application.ApplicationLenses.ApplicationLensOps
import models.application.{Application, Approved, Credential, Secret}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object GeneratePrimarySecretSuccessViewModel {

  def buildSummary(
    application: Application,
    environmentNames: EnvironmentNames,
    credential: Credential,
    secret: Secret
  )(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows = Seq(
        applicationNameRow(application),
        environmentNameRow(environmentNames),
        scopesRow(application),
        clientIdRow(credential),
        clientSecretRow(secret)
      )
    )
  }

  private def applicationNameRow(application: Application)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "generatePrimarySecretSuccess.applicationName",
      value   = ValueViewModel(HtmlFormat.escape(application.name).toString),
      actions = Seq.empty
    )
  }

  private def environmentNameRow(environmentNames: EnvironmentNames)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "generatePrimarySecretSuccess.environment",
      value   = ValueViewModel(HtmlFormat.escape(environmentNames.primary).toString),
      actions = Seq.empty
    )
  }

  private def scopesRow(application: Application)(implicit messages: Messages): SummaryListRow = {
    val scopeNames = application.getPrimaryScopes
      .filter(_.status == Approved)
      .map(_.name)
      .mkString(", ")

    SummaryListRowViewModel(
      key     = "generatePrimarySecretSuccess.scopes",
      value   = ValueViewModel(HtmlFormat.escape(scopeNames).toString),
      actions = Seq.empty
    )
  }

  private def clientIdRow(credential: Credential)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "generatePrimarySecretSuccess.clientId",
      value   = ValueViewModel(HtmlFormat.escape(credential.clientId).toString),
      actions = Seq.empty
    )
  }

  private def clientSecretRow(secret: Secret)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "generatePrimarySecretSuccess.clientSecret",
      value   = ValueViewModel(HtmlFormat.escape(secret.secret).toString),
      actions = Seq(
        ActionItem(
          href = "#",
          content = Text("Copy"),
          classes = "govuk-button",
          attributes = Map("id" -> "copy-to-clipboard-button")
        )
      )
    )
  }
}
