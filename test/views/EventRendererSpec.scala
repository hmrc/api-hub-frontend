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

package views

import models.event.{Created, Event, Team}
import models.team.TeamType
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{Format, Json}
import play.twirl.api.Html

import java.time.LocalDateTime

class EventRendererSpec extends AnyFreeSpec with Matchers {

  import EventRendererSpec.*

  "EventRenderer" - {
    "must correctly render a create team event" in {
      val event = Event(
        id = "test-event-id",
        entityId = "test-team-id",
        entityType = Team,
        eventType = Created,
        user = "test-user",
        timestamp = LocalDateTime.now(),
        description = "",
        detail = "",
        parameters = Json.parse(createTeamJson)
      )

      val eventRenderer = new EventRenderer()

      println(eventRenderer.renderEvent(event))
    }
  }

}

private object EventRendererSpec {

  /*
    Example event:
      Event type: Team member added
      Date: 17 September 2024 at 11:04 am
      Team member: another.teammember@hmrc.gov.uk
      Added by: another.teammember@hmrc.gov.uk

    The representation of this event is:

    EventRepresentation
      eventTypeValueKey:  message file key for "Team member added"
      userLabelKey:       message file key for "Added by"
      fields:
        EventField
          labelKey:       message file key for "Team member"
          html:           another.teammember@hmrc.gov.uk
          supportOnly:    false
   */

  // Replaces description and details. We can have zero or more fields that source values
  // from the parameters.
  case class EventField(
    labelKey: String,
    html: Html,
    supportOnly: Boolean
  )

  case class EventRepresentation(
    eventTypeValueKey: String,
    userLabelKey: String,
    fields: Seq[EventField]
  )

  object EventRepresentation {

    // Representation when the parameters JSON cannot be transformed to a model
    def unreadable(eventTypeKey: String, userKey: String): EventRepresentation = {
      EventRepresentation(
        eventTypeValueKey = eventTypeKey,
        userLabelKey = userKey,
        fields = Seq.empty
      )
    }

  }

  class EventRenderer {

    // Main entry point, call from twirl templates
    def renderEvent(event: Event): EventRepresentation = {
      (event.entityType, event.eventType) match {
        case (Team, Created) => renderTeamCreated(event)
        case _ => ???
      }
    }

    // Each event type gets its own implementation
    private def renderTeamCreated(event: Event): EventRepresentation = {
      val eventTypeKey = "event.team.created.eventType"
      val userKey = "event.team.created.user"

      event.parameters.validate[CreateTeamEventParameters].fold(
        invalid => EventRepresentation.unreadable(eventTypeKey, userKey),
        parameters => {
          val teamMembersHtml = unorderedList(parameters.teamMembers)

          EventRepresentation(
            eventTypeValueKey = eventTypeKey,
            userLabelKey = userKey,
            fields = Seq(
              EventField(
                labelKey = "event.team.created.teamName",
                html = Html(parameters.teamName),
                supportOnly = false
              ),
              EventField(
                labelKey = "event.team.created.teamMembers",
                html = teamMembersHtml,
                supportOnly = false
              ),
              EventField(
                labelKey = "event.team.created.teamType",
                html = Html(parameters.teamType.toString),
                supportOnly = true
              )
            )
          )
        }
      )
    }

    // We could use some utility functions to abstract HTML best practice
    private def unorderedList(items: Seq[String]): Html = {
      val listItems = items.map(
        item =>
          s"<li>$item</li"
      )

      Html(s"""<ul class="govuk-list govuk-list--bullet">${listItems.mkString}</ul>""")
    }

  }

  // Example parameters JSON for a create team event
  val createTeamJson: String =
    """
      |{
      |  "teamName": "My team",
      |  "teamMembers": ["jo.bloggs@hmrc.gov.uk", "john.doe@hmrc.gov.uk"],
      |  "teamType": "consumer"
      |}
      |""".stripMargin

  // We can optionally create a model for the parameters in the FE only
  case class CreateTeamEventParameters(teamName: String, teamMembers: Seq[String], teamType: TeamType)

  object CreateTeamEventParameters {
    implicit val formatCreateTeamEventParameters: Format[CreateTeamEventParameters] = Json.format[CreateTeamEventParameters]
  }

}
