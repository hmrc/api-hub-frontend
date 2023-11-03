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

import models.application.{Application, Credential}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AddCredentialSuccessViewModel {

  def buildSummary(
    application: Application,
    apiNames: Seq[String],
    credential: Credential
  )(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows = Seq(
        applicationNameRow(application),
        apisRow(apiNames),
        environmentNameRow(),
        clientIdRow(credential),
        clientSecretRow(credential)
      )
    )
  }

  private def applicationNameRow(application: Application)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "addCredentialSuccess.applicationName",
      value   = ValueViewModel(HtmlFormat.escape(application.name).toString),
      actions = Seq.empty
    )
  }

  private def apisRow(apiNames: Seq[String])(implicit messages: Messages): SummaryListRow = {
    val listItems = apiNames.map(
      name =>
        s"<li>$name</li>"
    ).mkString

    SummaryListRowViewModel(
      key     = "addCredentialSuccess.apis",
      value   = ValueViewModel(HtmlContent(s"<ul class=\"govuk-list govuk-list--bullet\">$listItems</ul>")),
      actions = Seq.empty
    )
  }

  private def environmentNameRow()(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "addCredentialSuccess.environment",
      value   = ValueViewModel(HtmlFormat.escape(messages("addCredentialSuccess.production")).toString),
      actions = Seq.empty
    )
  }

  private def clientIdRow(credential: Credential)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "addCredentialSuccess.clientId",
      value   = ValueViewModel(HtmlFormat.escape(credential.clientId).toString),
      actions = Seq(
        ActionItem(
          href = "#",
          content = Text("Copy"),
          classes = "govuk-button",
          attributes = Map("id" -> "copy-client-id-button")
        )
      )
    )
  }

  private def clientSecretRow(credential: Credential)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key     = "addCredentialSuccess.clientSecret",
      value   = ValueViewModel(HtmlFormat.escape(credential.clientSecret.getOrElse("")).toString),
      actions = Seq(
        ActionItem(
          href = "#",
          content = Text("Copy"),
          classes = "govuk-button",
          attributes = Map("id" -> "copy-client-secret-button")
        )
      )
    )
  }

}
