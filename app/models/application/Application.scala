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

package models.application

import play.api.libs.json.{Format, Json}

import java.time.LocalDateTime

case class Application (
  id: String,
  name: String,
  created: LocalDateTime,
  createdBy: Creator,
  lastUpdated: LocalDateTime,
  teamMembers: Seq[TeamMember],
  environments: Environments
)

object Application {

  def apply(id: String, name: String, createdBy: Creator, teamMembers: Seq[TeamMember]): Application = {
    val now = LocalDateTime.now()
    Application(id, name, now, createdBy, now, Seq.empty, Environments())
  }

  def apply(id: String, newApplication: NewApplication): Application = {
    apply(id, newApplication.name, newApplication.createdBy, newApplication.teamMembers)
  }

  def apply(id: String, name: String, createdBy: Creator, environments: Environments): Application = {
    val now = LocalDateTime.now()
    Application(id, name, now, createdBy, now, Seq.empty, environments)
  }

  implicit val applicationFormat: Format[Application] = Json.format[Application]

}
