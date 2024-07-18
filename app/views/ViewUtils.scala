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

package views

import models.accessrequest._
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

object ViewUtils {

  def title(form: Form[_], title: String, section: Option[String] = None)(implicit messages: Messages): String =
    titleNoForm(
      title   = s"${errorPrefix(form)} ${messages(title)}",
      section = section
    )

  def titleNoForm(title: String, section: Option[String] = None)(implicit messages: Messages): String =
    s"${messages(title)} - ${section.fold("")(messages(_) + " - ")}${messages("service.name")} - ${messages("site.govuk")}"

  def errorPrefix(form: Form[_])(implicit messages: Messages): String = {
    if (form.hasErrors || form.hasGlobalErrors) messages("error.browser.title.prefix") else ""
  }

  def formatLocalDateTimeContainingUtc(value: LocalDateTime): String = {
    value.format(DateTimeFormatter.ISO_DATE_TIME)
  }

  def formatInstantAsUtc(instant: Instant): String = {
    instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
  }

  def formatAccessRequestStatus(status: AccessRequestStatus): Html = {
    status match {
      case Pending => Html(s"<strong class='govuk-tag govuk-tag--yellow'>${status.toString}</strong>")
      case Approved => Html(s"<strong class='govuk-tag govuk-tag--green'>${status.toString}</strong>")
      case Rejected => Html(s"<strong class='govuk-tag govuk-tag--red'>${status.toString}</strong>")
      case Cancelled => Html(s"<strong class='govuk-tag govuk-tag--grey'>${status.toString}</strong>")
    }
  }

}
