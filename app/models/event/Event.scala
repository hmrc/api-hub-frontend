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

package models.event

import play.api.libs.json.{Format, JsValue, Json}

import java.time.LocalDateTime

case class Event(
                  id: String,
                  entityId: String,
                  entityType: EntityType,
                  eventType: EventType,
                  user: String,
                  timestamp: LocalDateTime,
                  description: String,
                  detail: String,
                  parameters: JsValue
                )
{

}

object Event {
  implicit val eventFormat: Format[Event] = Json.format[Event]
}


