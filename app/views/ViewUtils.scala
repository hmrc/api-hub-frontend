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

import models.accessrequest.{AccessRequestStatus, Approved, Cancelled, Pending, Rejected}
import models.application.Application
import models.application.ApplicationLenses._
import models.user.UserModel
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html

import java.time.LocalDateTime
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

  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm")
  private val shortDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
  private val shortestDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy - HH:mm")
  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

  def formatLocalDateTime(value: LocalDateTime): String = {
    value.format(dateTimeFormatter)
  }

  def formatShortLocalDateTime(value: LocalDateTime): String = {
    value.format(shortDateTimeFormatter)
  }

  def formatShortestLocalDateTime(value: LocalDateTime): String = {
    value.format(shortestDateTimeFormatter)
  }

  def formatDate(value: LocalDateTime): String = {
    s"${value.format(dateFormatter)}"
  }

  def formatShortDate(value: LocalDateTime): String = {
    s"${value.format(shortDateFormatter)}"
  }

  def formatAccessRequestStatus(status: AccessRequestStatus): Html = {
    status match {
      case Pending => Html(s"<strong class='govuk-tag govuk-tag--yellow'>${status.toString}</strong>")
      case Approved => Html(s"<strong class='govuk-tag govuk-tag--green'>${status.toString}</strong>")
      case Rejected => Html(s"<strong class='govuk-tag govuk-tag--red'>${status.toString}</strong>")
      case Cancelled => Html(s"<strong class='govuk-tag govuk-tag--grey'>${status.toString}</strong>")
    }
  }

  def isTeamMember(user: Option[UserModel], application: Application): Boolean = {
    user.flatMap(
      _.email.map(application.hasTeamMember)
    ).getOrElse(false)
  }

}
